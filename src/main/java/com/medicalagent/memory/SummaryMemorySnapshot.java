package com.medicalagent.memory;

import java.time.Instant;

public record SummaryMemorySnapshot(
        String sessionId,
        String summary,
        Instant updatedAt
) {
}
