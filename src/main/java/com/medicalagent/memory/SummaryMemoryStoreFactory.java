package com.medicalagent.memory;

import com.medicalagent.config.MemoryConfig;

import java.nio.file.Path;

public class SummaryMemoryStoreFactory {

    public SummaryMemoryStore create(MemoryConfig memoryConfig) {
        if (!"sqlite".equals(memoryConfig.getSummaryStore())) {
            throw new IllegalArgumentException("Unsupported summary memory store: " + memoryConfig.getSummaryStore());
        }
        return new SqliteSummaryMemoryStore(Path.of(memoryConfig.getSummarySqlitePath()));
    }
}
