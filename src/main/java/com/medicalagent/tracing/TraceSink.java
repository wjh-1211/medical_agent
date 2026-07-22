package com.medicalagent.tracing;

import java.util.List;

public interface TraceSink {

    void record(TraceEvent event);

    default List<TraceEvent> findByTraceId(String traceId) {
        return List.of();
    }
}
