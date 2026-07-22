package com.medicalagent.tracing;

import com.medicalagent.config.TracingConfig;

import java.util.List;

public class TraceRuntimeFactory {

    public TraceRuntime create(TracingConfig config) {
        InMemoryTraceSink memorySink = new InMemoryTraceSink();
        if (!config.isEnabled()) {
            return new TraceRuntime(event -> { }, memorySink);
        }
        return new TraceRuntime(new CompositeTraceSink(List.of(memorySink, new StructuredLogTraceSink())), memorySink);
    }
}
