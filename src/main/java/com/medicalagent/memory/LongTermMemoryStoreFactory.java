package com.medicalagent.memory;

import com.medicalagent.config.MemoryConfig;

import java.nio.file.Path;

public class LongTermMemoryStoreFactory {

    public LongTermMemoryStore create(MemoryConfig memoryConfig) {
        if (!"sqlite".equals(memoryConfig.getLongTermStore())) {
            throw new IllegalArgumentException("Unsupported long-term memory store: " + memoryConfig.getLongTermStore());
        }
        return new SqliteLongTermMemoryStore(Path.of(memoryConfig.getLongTermSqlitePath()));
    }
}
