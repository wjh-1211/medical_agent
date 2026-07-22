package com.medicalagent.tracing;

public record TraceRuntime(TraceSink sink, InMemoryTraceSink memorySink) {
}
