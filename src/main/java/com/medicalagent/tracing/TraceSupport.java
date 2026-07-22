package com.medicalagent.tracing;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.model.LocalModelException;

import java.time.Instant;
import java.util.Map;

public final class TraceSupport {

    private TraceSupport() {
    }

    public static TraceEvent event(
            String traceId,
            TraceEventType type,
            String name,
            TraceStatus status,
            long durationMillis,
            Throwable error,
            String input,
            String output,
            Map<String, String> attributes,
            int maxPayloadCharacters
    ) {
        return new TraceEvent(
                traceId,
                Instant.now(),
                type,
                name,
                status,
                durationMillis,
                classify(error),
                summarize(input, maxPayloadCharacters),
                summarize(output, maxPayloadCharacters),
                attributes
        );
    }

    public static String summarizeJson(JsonNode value, int maxPayloadCharacters) {
        try {
            return summarize(JsonSupport.JSON_MAPPER.writeValueAsString(value), maxPayloadCharacters);
        } catch (Exception exception) {
            return "<json-summary-failed>";
        }
    }

    public static TraceErrorCategory classify(Throwable error) {
        if (error == null) {
            return TraceErrorCategory.NONE;
        }
        String message = error.getMessage() == null ? "" : error.getMessage().toLowerCase();
        if (message.contains("timeout") || message.contains("timed out")) {
            return TraceErrorCategory.MODEL_TIMEOUT;
        }
        if (error instanceof LocalModelException) {
            return TraceErrorCategory.MODEL_TRANSPORT;
        }
        if (message.contains("model response") || message.contains("decision type")) {
            return TraceErrorCategory.MODEL_INVALID_OUTPUT;
        }
        if (message.contains("tool") || message.contains("skill")) {
            return TraceErrorCategory.TOOL_EXECUTION;
        }
        return TraceErrorCategory.UNKNOWN;
    }

    public static String summarize(String value, int maxPayloadCharacters) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxPayloadCharacters) {
            return compact;
        }
        return compact.substring(0, Math.max(0, maxPayloadCharacters - 3)) + "...";
    }
}
