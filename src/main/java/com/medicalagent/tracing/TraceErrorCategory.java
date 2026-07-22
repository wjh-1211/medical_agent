package com.medicalagent.tracing;

public enum TraceErrorCategory {
    NONE,
    MODEL_TIMEOUT,
    MODEL_TRANSPORT,
    MODEL_INVALID_OUTPUT,
    TOOL_EXECUTION,
    UNKNOWN
}
