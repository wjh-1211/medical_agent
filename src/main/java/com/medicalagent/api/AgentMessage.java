package com.medicalagent.api;

public record AgentMessage(
        String role,
        String content
) {
}
