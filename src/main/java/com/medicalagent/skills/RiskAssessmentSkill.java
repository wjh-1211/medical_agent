package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;

import java.util.Set;

/** Validates and normalizes an LLM-produced non-emergency risk decision. */
public class RiskAssessmentSkill implements Skill {

    public static final String TOOL_NAME = "risk_assessment";
    private static final Set<String> RISK_LEVELS = Set.of("low", "moderate", "high");

    @Override
    public String id() {
        return "riskAssessmentSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("message").put("type", "string");
        properties.putObject("memorySummary").put("type", "string");
        properties.putObject("riskLevel").put("type", "string");
        properties.putObject("requiresFollowUp").put("type", "boolean");
        properties.putObject("reason").put("type", "string");
        schema.putArray("required").add("message").add("memorySummary").add("riskLevel").add("requiresFollowUp").add("reason");
        return new ToolSchema(TOOL_NAME, "Validate a structured medical risk decision without diagnosing or prescribing.", schema);
    }

    @Override
    public JsonNode execute(JsonNode input) {
        String riskLevel = input.path("riskLevel").asText().trim().toLowerCase();
        String reason = input.path("reason").asText().trim();
        if (!RISK_LEVELS.contains(riskLevel)) {
            throw new IllegalArgumentException("Unsupported risk level: " + riskLevel);
        }
        if (reason.isBlank()) {
            throw new IllegalArgumentException("Risk decision reason must not be blank");
        }
        boolean requiresFollowUp = input.path("requiresFollowUp").asBoolean();
        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("status", "ok");
        output.put("riskLevel", riskLevel);
        output.put("requiresFollowUp", requiresFollowUp);
        output.put("reason", reason);
        output.put("action", "high".equals(riskLevel) || requiresFollowUp ? "limit" : "continue");
        return output;
    }
}
