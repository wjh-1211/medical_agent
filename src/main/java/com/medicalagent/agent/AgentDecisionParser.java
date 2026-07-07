package com.medicalagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;

public class AgentDecisionParser {

    public AgentDecision parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("Model response must not be blank");
        }

        JsonNode root;
        try {
            root = JsonSupport.JSON_MAPPER.readTree(rawContent);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Model response must be valid JSON", exception);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Model response must be a JSON object");
        }

        String rawType = root.path("type").asText();
        if (rawType == null || rawType.isBlank()) {
            throw new IllegalArgumentException("Model response missing decision type");
        }

        AgentDecision.Type type = AgentDecision.Type.fromWireValue(rawType);
        return switch (type) {
            case FINAL_ANSWER -> parseFinalAnswer(root);
            case TOOL_CALL -> parseToolCall(root);
        };
    }

    private AgentDecision parseFinalAnswer(JsonNode root) {
        String answer = root.path("answer").asText();
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("Model response missing final answer text");
        }
        return AgentDecision.finalAnswer(answer.trim());
    }

    private AgentDecision parseToolCall(JsonNode root) {
        String toolName = root.path("toolName").asText();
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Model response missing toolName for tool_call");
        }
        JsonNode input = root.get("input");
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException("Model response input must be a JSON object for tool_call");
        }
        return AgentDecision.toolCall(toolName.trim(), input);
    }
}
