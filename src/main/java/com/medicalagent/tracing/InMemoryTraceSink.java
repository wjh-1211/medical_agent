package com.medicalagent.tracing;

import java.util.ArrayList;
import java.util.List;

public class InMemoryTraceSink implements TraceSink {

    private final List<TraceEvent> events = new ArrayList<>();

    @Override
    public synchronized void record(TraceEvent event) {
        events.add(event);
    }

    @Override
    public synchronized List<TraceEvent> findByTraceId(String traceId) {
        return events.stream().filter(event -> event.traceId().equals(traceId)).toList();
    }
}
