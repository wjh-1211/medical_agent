package com.medicalagent.memory;

import java.time.Instant;

public record SessionMemorySnapshot(
        String sessionId,
        String memorySummary,
        String lastUserMessage,
        String lastAgentAnswer,
        Instant updatedAt,
        Instant expiresAt
) {
}
