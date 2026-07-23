package com.medicalagent.knowledge;

import java.util.List;

public record KnowledgeSearchTrace(
        List<KnowledgeChunkMatch> matches,
        long embeddingMillis,
        long retrievalMillis,
        long lexicalMillis,
        long fusionMillis,
        long rerankMillis,
        String strategy
) {
    public KnowledgeSearchTrace(List<KnowledgeChunkMatch> matches, long embeddingMillis, long retrievalMillis) {
        this(matches, embeddingMillis, retrievalMillis, 0L, 0L, 0L, "vector");
    }
}
