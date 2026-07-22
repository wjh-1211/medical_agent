package com.medicalagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.context.AgentContext;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EmergencyDetectionSkill;
import com.medicalagent.skills.GroundingCheckSkill;
import com.medicalagent.skills.RiskAssessmentSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.ToolSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Applies ordered Guardrail Tools after a candidate answer has been generated. */
public class GuardrailPipeline {

    private final AppConfig config;
    private final SkillRegistry skillRegistry;
    private final ToolRouter toolRouter;
    private final GuardrailDecisionAgent decisionAgent;

    public GuardrailPipeline(
            AppConfig config,
            SkillRegistry skillRegistry,
            ToolRouter toolRouter,
            GuardrailDecisionAgent decisionAgent
    ) {
        this.config = config;
        this.skillRegistry = skillRegistry;
        this.toolRouter = toolRouter;
        this.decisionAgent = decisionAgent;
    }

    public GuardrailOutcome apply(AgentContext context, String candidateAnswer, List<JsonNode> observations) {
        List<JsonNode> results = new ArrayList<>();
        Optional<JsonNode> emergencyResult = runEmergency(context, candidateAnswer, observations);
        emergencyResult.ifPresent(results::add);
        JsonNode emergency = emergencyResult.orElse(null);
        if (emergency != null && emergency.path("emergency").asBoolean()) {
            return new GuardrailOutcome(config.getGuardrail().getEmergencySafeAnswer(), true, results);
        }
        if (config.getGuardrail().isBlockOnDecisionFailure()
                && isActive(config.getGuardrail().isEmergencyDetectionEnabled(), EmergencyDetectionSkill.TOOL_NAME)
                && (emergency == null || emergency.path("decisionFailed").asBoolean())) {
            return new GuardrailOutcome(config.getGuardrail().getDecisionFailureSafeAnswer(), true, results);
        }

        Optional<JsonNode> riskResult = runRisk(context, candidateAnswer, observations);
        riskResult.ifPresent(results::add);
        JsonNode risk = riskResult.orElse(null);
        if (risk != null && "limit".equals(risk.path("action").asText())) {
            return new GuardrailOutcome(config.getGuardrail().getHighRiskSafeAnswer(), false, results);
        }
        if (config.getGuardrail().isBlockOnDecisionFailure()
                && isActive(config.getGuardrail().isRiskAssessmentEnabled(), RiskAssessmentSkill.TOOL_NAME)
                && (risk == null || risk.path("decisionFailed").asBoolean())) {
            return new GuardrailOutcome(config.getGuardrail().getDecisionFailureSafeAnswer(), false, results);
        }

        Optional<JsonNode> groundingResult = runGrounding(context, candidateAnswer, observations);
        groundingResult.ifPresent(results::add);
        JsonNode grounding = groundingResult.orElse(null);
        if (config.getGuardrail().isBlockOnDecisionFailure()
                && isActive(config.getGuardrail().isGroundingCheckEnabled(), GroundingCheckSkill.TOOL_NAME)
                && (grounding == null || grounding.path("decisionFailed").asBoolean())) {
            return new GuardrailOutcome(config.getGuardrail().getDecisionFailureSafeAnswer(), true, results);
        }
        if (grounding != null && "limit".equals(grounding.path("action").asText())) {
            return new GuardrailOutcome(config.getGuardrail().getInsufficientEvidenceSafeAnswer(), false, results);
        }
        return new GuardrailOutcome(candidateAnswer, false, results);
    }

    private Optional<JsonNode> runEmergency(AgentContext context, String answer, List<JsonNode> observations) {
        if (!config.getGuardrail().isEmergencyDetectionEnabled()) {
            return Optional.empty();
        }
        return run(
                "emergency-detection",
                EmergencyDetectionSkill.TOOL_NAME,
                context,
                answer,
                observations,
                input -> {
                    input.put("message", context.getMessage());
                    input.put("emergency", true);
                    input.put("urgency", "emergency");
                    input.put("reason", "guardrail_decision_failed");
                }
        );
    }

    private Optional<JsonNode> runRisk(AgentContext context, String answer, List<JsonNode> observations) {
        if (!config.getGuardrail().isRiskAssessmentEnabled()) {
            return Optional.empty();
        }
        return run(
                "risk-assessment",
                RiskAssessmentSkill.TOOL_NAME,
                context,
                answer,
                observations,
                input -> {
                    input.put("message", context.getMessage());
                    input.put("memorySummary", context.getMemorySummary() == null ? "" : context.getMemorySummary());
                    input.put("riskLevel", "high");
                    input.put("requiresFollowUp", true);
                    input.put("reason", "guardrail_decision_failed");
                }
        );
    }

    private Optional<JsonNode> runGrounding(AgentContext context, String answer, List<JsonNode> observations) {
        if (!config.getGuardrail().isGroundingCheckEnabled()) {
            return Optional.empty();
        }
        return run(
                "grounding-check",
                GroundingCheckSkill.TOOL_NAME,
                context,
                answer,
                observations,
                input -> {
                    input.put("candidateAnswer", answer);
                    input.set("observations", observationsArray(observations));
                    input.put("requiresEvidence", true);
                    input.put("reason", "guardrail_decision_failed");
                }
        );
    }

    private Optional<JsonNode> run(
            String promptName,
            String toolName,
            AgentContext context,
            String answer,
            List<JsonNode> observations,
            java.util.function.Consumer<ObjectNode> failureInput
    ) {
        Optional<ToolSchema> schema = skillRegistry.findSchemaByToolName(toolName);
        if (schema.isEmpty()) {
            return Optional.empty();
        }
        Optional<JsonNode> decision = decisionAgent.decide(promptName, context, answer, observations, schema.get());
        ObjectNode input = decision.filter(JsonNode::isObject)
                .map(node -> (ObjectNode) node.deepCopy())
                .orElseGet(JsonSupport.NODE_FACTORY::objectNode);
        addExecutionFields(toolName, context, answer, observations, input);
        boolean decisionFailed = decision.isEmpty();
        if (decisionFailed) {
            failureInput.accept(input);
        }
        try {
            JsonNode result = toolRouter.route(toolName, input);
            if (decisionFailed && result instanceof ObjectNode objectResult) {
                objectResult.put("decisionFailed", true);
            }
            return Optional.of(result);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private void addExecutionFields(
            String toolName,
            AgentContext context,
            String answer,
            List<JsonNode> observations,
            ObjectNode input
    ) {
        if (EmergencyDetectionSkill.TOOL_NAME.equals(toolName)) {
            input.put("message", context.getMessage());
        } else if (RiskAssessmentSkill.TOOL_NAME.equals(toolName)) {
            input.put("message", context.getMessage());
            input.put("memorySummary", context.getMemorySummary() == null ? "" : context.getMemorySummary());
        } else if (GroundingCheckSkill.TOOL_NAME.equals(toolName)) {
            input.put("candidateAnswer", answer);
            input.set("observations", observationsArray(observations));
        }
    }

    private ArrayNode observationsArray(List<JsonNode> observations) {
        ArrayNode array = JsonSupport.NODE_FACTORY.arrayNode();
        observations.forEach(observation -> array.add(observation.deepCopy()));
        return array;
    }

    private boolean isActive(boolean enabled, String toolName) {
        return enabled && skillRegistry.findSchemaByToolName(toolName).isPresent();
    }
}
