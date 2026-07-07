package com.medicalagent.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.medicalagent.api.AgentMessage;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.context.AgentContext;
import com.medicalagent.skills.ToolSchema;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromptVariablesFactory {

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
        return history.stream()
                .map(message -> message.role() + ": " + message.content())
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatJson(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        try {
            return JsonSupport.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
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
        try {
            return JsonSupport.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(availableTools);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize availableTools to prompt variables", exception);
        }
    }

    private String formatObservations(List<JsonNode> observations) {
        if (observations == null || observations.isEmpty()) {
            return "No observations yet.";
        }
        return observations.stream()
                .map(this::formatJson)
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
