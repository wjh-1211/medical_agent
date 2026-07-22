package com.medicalagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.context.AgentContext;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.prompt.PromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptTemplate;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.skills.ToolSchema;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/** Uses a dedicated prompt to produce one structured guardrail Tool input. */
public class GuardrailDecisionAgent {

    private static final Logger LOGGER = Logger.getLogger(GuardrailDecisionAgent.class.getName());

    private final PromptLoader promptLoader;
    private final PromptVariablesFactory promptVariablesFactory;
    private final PromptRenderer promptRenderer;
    private final LocalModelGateway localModelGateway;
    private final AgentDecisionParser agentDecisionParser;
    private final int modelCallTimeoutMillis;

    public GuardrailDecisionAgent(
            PromptLoader promptLoader,
            PromptVariablesFactory promptVariablesFactory,
            PromptRenderer promptRenderer,
            LocalModelGateway localModelGateway,
            AgentDecisionParser agentDecisionParser,
            int modelCallTimeoutMillis
    ) {
        this.promptLoader = promptLoader;
        this.promptVariablesFactory = promptVariablesFactory;
        this.promptRenderer = promptRenderer;
        this.localModelGateway = localModelGateway;
        this.agentDecisionParser = agentDecisionParser;
        this.modelCallTimeoutMillis = modelCallTimeoutMillis;
    }

    public Optional<JsonNode> decide(
            String promptName,
            AgentContext context,
            String candidateAnswer,
            List<JsonNode> observations,
            ToolSchema toolSchema
    ) {
        try {
            PromptTemplate template = promptLoader.load(promptName);
            Map<String, String> variables = promptVariablesFactory.create(context, List.of(toolSchema), observations);
            variables.put("candidateAnswer", candidateAnswer);
            String prompt = promptRenderer.render(template, variables);
            String content = localModelGateway.generate(new LocalModelRequest(
                    prompt,
                    localModelGateway.descriptor().getName(),
                    localModelGateway.descriptor().getPath(),
                    localModelGateway.descriptor().getTemperature(),
                    localModelGateway.descriptor().getMaxTokens(),
                    true,
                    modelCallTimeoutMillis
            )).content();
            try {
                AgentDecision decision = agentDecisionParser.parse(content);
                if (decision.type() == AgentDecision.Type.TOOL_CALL && toolSchema.name().equals(decision.toolName())) {
                    return Optional.of(decision.input());
                }
            } catch (IllegalArgumentException ignored) {
                // Some local models follow the Guardrail schema directly instead of the shared tool-call envelope.
            }
            JsonNode directDecision = JsonSupport.JSON_MAPPER.readTree(content);
            if (directDecision == null || !directDecision.isObject() || !containsDecisionFields(directDecision, toolSchema.name())) {
                throw new IllegalArgumentException("Guardrail decision must be a JSON object for tool: " + toolSchema.name());
            }
            return Optional.of(directDecision);
        } catch (Exception exception) {
            LOGGER.warning(() -> "Guardrail decision failed for " + promptName + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    private boolean containsDecisionFields(JsonNode input, String toolName) {
        String[] requiredFields = switch (toolName) {
            case "emergency_detection" -> new String[]{"emergency", "urgency", "reason"};
            case "risk_assessment" -> new String[]{"riskLevel", "requiresFollowUp", "reason"};
            case "grounding_check" -> new String[]{"requiresEvidence", "reason"};
            default -> new String[0];
        };
        for (String field : requiredFields) {
            if (!input.hasNonNull(field)) {
                return false;
            }
        }
        return true;
    }
}
