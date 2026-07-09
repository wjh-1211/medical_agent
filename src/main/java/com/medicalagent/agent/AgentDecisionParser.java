package com.medicalagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;

import java.util.ArrayList;
import java.util.List;

public class AgentDecisionParser {

    public AgentDecision parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("Model response must not be blank");
        }

        JsonNode root = parseRootObject(rawContent);
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

    private JsonNode parseRootObject(String rawContent) {
        try {
            return JsonSupport.JSON_MAPPER.readTree(rawContent);
        } catch (Exception directParseException) {
            JsonNode extractedRoot = extractDecisionObject(rawContent);
            if (extractedRoot == null) {
                throw new IllegalArgumentException(buildInvalidJsonMessage(rawContent), directParseException);
            }
            return extractedRoot;
        }
    }

    private JsonNode extractDecisionObject(String rawContent) {
        List<String> candidates = extractJsonObjectCandidates(stripMarkdownFences(rawContent));
        for (int index = candidates.size() - 1; index >= 0; index--) {
            JsonNode parsed = tryParseJsonObject(candidates.get(index));
            if (parsed == null) {
                continue;
            }
            String type = parsed.path("type").asText();
            if (type != null && !type.isBlank()) {
                return parsed;
            }
        }
        return null;
    }

    private List<String> extractJsonObjectCandidates(String rawContent) {
        List<String> candidates = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        int start = -1;
        for (int index = 0; index < rawContent.length(); index++) {
            char current = rawContent.charAt(index);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (current == '\\') {
                escaping = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                if (depth == 0) {
                    start = index;
                }
                depth++;
            } else if (current == '}') {
                if (depth == 0) {
                    continue;
                }
                depth--;
                if (depth == 0 && start >= 0) {
                    candidates.add(rawContent.substring(start, index + 1));
                    start = -1;
                }
            }
        }
        return candidates;
    }

    private JsonNode tryParseJsonObject(String candidate) {
        try {
            JsonNode parsed = JsonSupport.JSON_MAPPER.readTree(candidate);
            return parsed != null && parsed.isObject() ? parsed : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String stripMarkdownFences(String rawContent) {
        return rawContent
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();
    }

    private String buildInvalidJsonMessage(String rawContent) {
        String compact = rawContent.replaceAll("\\s+", " ").trim();
        if (compact.length() > 200) {
            compact = compact.substring(0, 200) + "...";
        }
        return "Model response must be valid JSON: " + compact;
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
