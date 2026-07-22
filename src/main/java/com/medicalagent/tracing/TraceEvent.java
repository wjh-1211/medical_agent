package com.medicalagent.tracing;

import java.time.Instant;
import java.util.Map;

public record TraceEvent(
        String traceId,
        Instant timestamp,
        TraceEventType type,
        String name,
        TraceStatus status,
        long durationMillis,
        TraceErrorCategory errorCategory,
        String inputSummary,
        String outputSummary,
        Map<String, String> attributes
) {
    public TraceEvent {
        attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
    }
}
