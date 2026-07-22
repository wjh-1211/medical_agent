package com.medicalagent.tracing;

import java.util.List;

public record CompositeTraceSink(List<TraceSink> sinks) implements TraceSink {

    public CompositeTraceSink {
        sinks = List.copyOf(sinks);
    }

    @Override
    public void record(TraceEvent event) {
        sinks.forEach(sink -> sink.record(event));
    }
}
