package com.medicalagent.tracing;

import java.util.Optional;

public final class TraceScope implements AutoCloseable {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private final String previousTraceId;

    private TraceScope(String traceId) {
        this.previousTraceId = TRACE_ID.get();
        TRACE_ID.set(traceId);
    }

    public static TraceScope open(String traceId) {
        return new TraceScope(traceId);
    }

    public static Optional<String> currentTraceId() {
        return Optional.ofNullable(TRACE_ID.get());
    }

    @Override
    public void close() {
        if (previousTraceId == null) {
            TRACE_ID.remove();
        } else {
            TRACE_ID.set(previousTraceId);
        }
    }
}
