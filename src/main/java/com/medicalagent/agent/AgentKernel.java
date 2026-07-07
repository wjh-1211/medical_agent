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
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.ToolSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    public AgentResponse handle(AgentContext context) {
        PromptTemplate promptTemplate = promptLoader.load(config.getPrompt().getDefaultTemplate());
        Collection<ToolSchema> availableTools = skillRegistry.registeredToolSchemas();
        List<JsonNode> observations = new ArrayList<>();
        int maxLoops = Math.max(1, config.getRuntime().getMaxReActLoops());
        String finalAnswer = null;

        for (int loop = 0; loop < maxLoops; loop++) {
            Map<String, String> promptVariables = promptVariablesFactory.create(context, availableTools, observations);
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
}
