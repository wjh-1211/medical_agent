package com.medicalagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.api.AgentResponse;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.context.AgentContext;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;
import com.medicalagent.prompt.PromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptTemplate;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.MemoryReadSkill;
import com.medicalagent.skills.MemoryWriteSkill;
import com.medicalagent.skills.LongTermMemoryReadSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.ToolSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Logger;

public record AgentKernel(
        AppConfig config,
        SkillRegistry skillRegistry,
        ToolRouter toolRouter,
        PromptLoader promptLoader,
        PromptVariablesFactory promptVariablesFactory,
        PromptRenderer promptRenderer,
        LocalModelGateway localModelGateway,
        AgentDecisionParser agentDecisionParser
) {

    private static final Logger LOGGER = Logger.getLogger(AgentKernel.class.getName());

    public AgentResponse handle(AgentContext context) {
        long requestStartedAt = System.nanoTime();
        JsonNode recalledSessionMemory = recallSessionMemory(context);
        JsonNode recalledLongTermMemory = recallLongTermMemory(context);
        AgentContext promptContext = augmentContextWithMemory(
                context,
                recalledSessionMemory,
                recalledLongTermMemory
        );
        PromptTemplate promptTemplate = promptLoader.load(config.getPrompt().getDefaultTemplate());
        Collection<ToolSchema> availableTools = skillRegistry.registeredToolSchemas();
        List<JsonNode> observations = new ArrayList<>();
        int maxLoops = Math.max(1, config.getRuntime().getMaxReActLoops());
        String finalAnswer = null;
        long totalModelMillis = 0L;
        int totalPromptCharacters = 0;
        int totalOutputCharacters = 0;
        int loopsUsed = 0;

        for (int loop = 0; loop < maxLoops; loop++) {
            Map<String, String> promptVariables = promptVariablesFactory.create(promptContext, availableTools, observations);
            String renderedPrompt = promptRenderer.render(promptTemplate, promptVariables);
            LocalModelResponse modelResponse = localModelGateway.generate(new LocalModelRequest(
                    renderedPrompt,
                    config.getModel().getName(),
                    config.getModel().getPath(),
                    config.getModel().getTemperature(),
                    config.getModel().getMaxTokens(),
                    config.getModel().isFunctionCallingEnabled(),
                    config.getTimeout().getModelCallMillis()
            ));
            totalModelMillis += modelResponse.elapsedMillis();
            totalPromptCharacters += modelResponse.promptCharacters();
            totalOutputCharacters += modelResponse.outputCharacters();
            loopsUsed = loop + 1;
            AgentDecision decision = agentDecisionParser.parse(modelResponse.content());
            if (decision.type() == AgentDecision.Type.FINAL_ANSWER) {
                finalAnswer = decision.answer();
                break;
            }
            observations.add(executeToolAndCreateObservation(decision));
        }

        if (finalAnswer == null) {
            throw new IllegalStateException("Agent did not reach final_answer within maxReActLoops=" + maxLoops);
        }

        new LongTermMemoryUpdateAgent(
                skillRegistry,
                toolRouter,
                promptLoader,
                promptVariablesFactory,
                promptRenderer,
                localModelGateway,
                agentDecisionParser,
                config.getTimeout().getModelCallMillis()
        ).update(context, finalAnswer);
        writeSessionMemory(context, finalAnswer);
        logPerformanceSummary(context, requestStartedAt, loopsUsed, totalModelMillis, totalPromptCharacters, totalOutputCharacters);

        return new AgentResponse(
                "ok",
                context.getRequestId(),
                context.getSessionId(),
                context.getUserId(),
                finalAnswer,
                context.getEmergencyFlag(),
                context.getCreatedAt().toString()
        );
    }

    private JsonNode executeToolAndCreateObservation(AgentDecision decision) {
        JsonNode toolResult;
        try {
            toolResult = toolRouter.route(decision.toolName(), decision.input());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Tool execution failed for tool: " + decision.toolName(), exception);
        }

        ObjectNode observation = JsonSupport.NODE_FACTORY.objectNode();
        observation.put("toolName", decision.toolName());
        observation.set("input", decision.input());
        observation.set("result", toolResult);
        return observation;
    }

    private JsonNode recallSessionMemory(AgentContext context) {
        if (skillRegistry.findRegistrationByToolName(MemoryReadSkill.TOOL_NAME).isEmpty()) {
            return JsonSupport.NODE_FACTORY.objectNode();
        }
        ObjectNode input = JsonSupport.NODE_FACTORY.objectNode();
        input.put("sessionId", context.getSessionId());
        return toolRouter.route(MemoryReadSkill.TOOL_NAME, input);
    }

    private JsonNode recallLongTermMemory(AgentContext context) {
        if (skillRegistry.findRegistrationByToolName(LongTermMemoryReadSkill.TOOL_NAME).isEmpty()) {
            return JsonSupport.NODE_FACTORY.objectNode();
        }
        ObjectNode input = JsonSupport.NODE_FACTORY.objectNode();
        input.put("userId", context.getUserId());
        return toolRouter.route(LongTermMemoryReadSkill.TOOL_NAME, input);
    }

    private void writeSessionMemory(AgentContext context, String finalAnswer) {
        if (skillRegistry.findRegistrationByToolName(MemoryWriteSkill.TOOL_NAME).isEmpty()) {
            return;
        }
        ObjectNode input = JsonSupport.NODE_FACTORY.objectNode();
        input.put("sessionId", context.getSessionId());
        input.put("userMessage", context.getMessage());
        input.put("agentAnswer", finalAnswer);
        toolRouter.route(MemoryWriteSkill.TOOL_NAME, input);
    }

    private AgentContext augmentContextWithMemory(
            AgentContext original,
            JsonNode recalledSessionMemory,
            JsonNode recalledLongTermMemory
    ) {
        boolean foundSessionMemory = recalledSessionMemory.path("found").asBoolean(false);
        boolean foundLongTermMemory = recalledLongTermMemory.path("found").asBoolean(false);
        if (!foundSessionMemory && !foundLongTermMemory) {
            return original;
        }
        String combinedSummary = original.getMemorySummary();
        if (foundSessionMemory) {
            combinedSummary = combineMemorySummaries(
                    combinedSummary,
                    recalledSessionMemory.path("memorySummary").asText()
            );
        }
        if (foundLongTermMemory) {
            combinedSummary = combineMemorySummaries(
                    combinedSummary,
                    summarizeLongTermMemory(recalledLongTermMemory)
            );
        }
        ObjectNode mergedToolFacts = original.getToolFacts().deepCopy();
        if (foundSessionMemory) {
            mergedToolFacts.set("sessionMemory", recalledSessionMemory);
        }
        if (foundLongTermMemory) {
            mergedToolFacts.set("longTermMemory", recalledLongTermMemory);
        }
        return AgentContext.builder()
                .requestId(original.getRequestId())
                .sessionId(original.getSessionId())
                .userId(original.getUserId())
                .message(original.getMessage())
                .history(original.getHistory())
                .memorySummary(combinedSummary)
                .toolFacts(mergedToolFacts)
                .emergencyFlag(original.getEmergencyFlag())
                .metadata(original.getMetadata())
                .createdAt(original.getCreatedAt())
                .requestTimeoutMillis(original.getRequestTimeoutMillis())
                .build();
    }

    private String summarizeLongTermMemory(JsonNode recalledLongTermMemory) {
        StringJoiner facts = new StringJoiner("; ");
        for (JsonNode record : recalledLongTermMemory.path("records")) {
            String category = record.path("category").asText();
            String fact = record.path("fact").asText();
            if (!category.isBlank() && !fact.isBlank()) {
                facts.add(category + ": " + fact);
            }
        }
        return facts.length() == 0 ? null : "Long-term user facts: " + facts;
    }

    private String combineMemorySummaries(String existing, String recalled) {
        String normalizedExisting = existing == null || existing.isBlank() ? null : existing.trim();
        String normalizedRecalled = recalled == null || recalled.isBlank() ? null : recalled.trim();
        if (normalizedExisting == null) {
            return normalizedRecalled;
        }
        if (normalizedRecalled == null) {
            return normalizedExisting;
        }
        return normalizedExisting + System.lineSeparator() + System.lineSeparator() + normalizedRecalled;
    }

    private void logPerformanceSummary(
            AgentContext context,
            long requestStartedAt,
            int loopsUsed,
            long totalModelMillis,
            int totalPromptCharacters,
            int totalOutputCharacters
    ) {
        if (!config.getRuntime().isToolLoggingEnabled()) {
            return;
        }
        if ("cli".equals(context.getMetadata().getOrDefault("channel", ""))) {
            return;
        }
        long totalElapsedMillis = (System.nanoTime() - requestStartedAt) / 1_000_000L;
        LOGGER.info(() -> "[perf] requestId=" + context.getRequestId()
                + " channel=" + context.getMetadata().getOrDefault("channel", "unknown")
                + " loops=" + loopsUsed
                + " totalMs=" + totalElapsedMillis
                + " modelMs=" + totalModelMillis
                + " promptChars=" + totalPromptCharacters
                + " outputChars=" + totalOutputCharacters);
    }
}
