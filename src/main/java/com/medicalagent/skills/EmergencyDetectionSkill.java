package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;

import java.util.Set;

/** Validates and normalizes an LLM-produced emergency decision. */
public class EmergencyDetectionSkill implements Skill {

    public static final String TOOL_NAME = "emergency_detection";
    private static final Set<String> URGENCY_LEVELS = Set.of("none", "urgent", "emergency");

    @Override
    public String id() {
        return "emergencyDetectionSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("message").put("type", "string");
        properties.putObject("emergency").put("type", "boolean");
        properties.putObject("urgency").put("type", "string");
        properties.putObject("reason").put("type", "string");
        schema.putArray("required").add("message").add("emergency").add("urgency").add("reason");
        return new ToolSchema(TOOL_NAME, "Validate a structured emergency safety decision without giving medical advice.", schema);
    }

    @Override
    public JsonNode execute(JsonNode input) {
        boolean emergency = input.path("emergency").asBoolean();
        String urgency = input.path("urgency").asText().trim().toLowerCase();
        String reason = input.path("reason").asText().trim();
        if (!URGENCY_LEVELS.contains(urgency)) {
            throw new IllegalArgumentException("Unsupported emergency urgency: " + urgency);
        }
        if (reason.isBlank()) {
            throw new IllegalArgumentException("Emergency decision reason must not be blank");
        }
        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("status", "ok");
        output.put("emergency", emergency);
        output.put("urgency", emergency && "none".equals(urgency) ? "urgent" : urgency);
        output.put("reason", reason);
        output.put("action", emergency ? "escalate" : "continue");
        return output;
    }
}
