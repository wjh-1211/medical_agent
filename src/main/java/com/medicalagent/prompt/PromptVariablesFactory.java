package com.medicalagent.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.medicalagent.api.AgentMessage;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.context.AgentContext;
import com.medicalagent.skills.ToolSchema;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromptVariablesFactory {

    private static final int MAX_HISTORY_MESSAGES = 4;
    private static final int MAX_OBSERVATIONS = 2;
    private static final int MAX_MESSAGE_LENGTH = 180;
    private static final int MAX_JSON_LENGTH = 1200;

    public Map<String, String> create(AgentContext context) {
        return create(context, List.of(), List.of());
    }

    public Map<String, String> create(AgentContext context, Collection<ToolSchema> availableTools, List<JsonNode> observations) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("requestId", context.getRequestId());
        variables.put("sessionId", context.getSessionId());
        variables.put("userId", context.getUserId());
        variables.put("message", context.getMessage());
        variables.put("history", formatHistory(context.getHistory()));
        variables.put("memorySummary", defaultText(context.getMemorySummary(), "N/A"));
        variables.put("toolFacts", formatJson(context.getToolFacts()));
        variables.put("emergencyFlag", context.getEmergencyFlag() == null ? "unknown" : context.getEmergencyFlag().toString());
        variables.put("metadata", formatMetadata(context.getMetadata()));
        variables.put("createdAt", context.getCreatedAt().toString());
        variables.put("availableTools", formatAvailableTools(availableTools));
        variables.put("observations", formatObservations(observations));
        return variables;
    }

    private String formatHistory(List<AgentMessage> history) {
        if (history == null || history.isEmpty()) {
            return "No prior history.";
        }
        List<AgentMessage> recentMessages = history.stream()
                .skip(Math.max(0, history.size() - MAX_HISTORY_MESSAGES))
                .toList();
        return recentMessages.stream()
                .map(message -> message.role() + ": " + compactText(message.content(), MAX_MESSAGE_LENGTH))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatJson(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        try {
            return compactText(JsonSupport.JSON_MAPPER.writeValueAsString(jsonNode), MAX_JSON_LENGTH);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize toolFacts to prompt variables", exception);
        }
    }

    private String formatMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "No metadata.";
        }
        return metadata.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatAvailableTools(Collection<ToolSchema> availableTools) {
        if (availableTools == null || availableTools.isEmpty()) {
            return "No tools registered.";
        }
        return availableTools.stream()
                .sorted(Comparator.comparing(ToolSchema::name))
                .map(this::formatToolSummary)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatObservations(List<JsonNode> observations) {
        if (observations == null || observations.isEmpty()) {
            return "No observations yet.";
        }
        List<JsonNode> recentObservations = observations.stream()
                .skip(Math.max(0, observations.size() - MAX_OBSERVATIONS))
                .toList();
        return recentObservations.stream()
                .map(this::formatJson)
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String formatToolSummary(ToolSchema schema) {
        JsonNode properties = schema.inputSchema().path("properties");
        List<String> argumentNames = extractPropertyNames(properties);
        String compactArguments = argumentNames.isEmpty() ? "" : "(" + String.join(", ", argumentNames) + ")";
        return "- " + schema.name() + compactArguments + ": " + compactText(schema.description(), 120);
    }

    private List<String> extractPropertyNames(JsonNode properties) {
        if (!properties.isObject()) {
            return List.of();
        }
        Iterator<String> fieldNames = properties.fieldNames();
        List<String> names = new java.util.ArrayList<>();
        while (fieldNames.hasNext()) {
            names.add(fieldNames.next());
        }
        return names;
    }

    private String compactText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
