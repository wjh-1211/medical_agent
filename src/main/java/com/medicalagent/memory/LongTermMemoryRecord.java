package com.medicalagent.memory;

import java.time.Instant;

public record LongTermMemoryRecord(
        String userId,
        LongTermMemoryCategory category,
        String fact,
        String source,
        Instant createdAt,
        Instant updatedAt
) {
}
