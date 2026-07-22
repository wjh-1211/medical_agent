package com.medicalagent.api;

public record AgentResponse(
        String status,
        String requestId,
        String sessionId,
        String userId,
        String traceId,
        String answer,
        Boolean emergencyFlag,
        String createdAt
) {
}
