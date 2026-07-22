package com.medicalagent.tracing;

public enum TraceEventType {
    REQUEST_STARTED,
    MODEL_CALL,
    TOOL_CALL,
    SWARM_PLAN,
    SWARM_ROLE,
    SWARM_MERGE,
    SWARM_FALLBACK,
    GUARDRAIL_ACTION,
    REQUEST_COMPLETED,
    REQUEST_FAILED
}
