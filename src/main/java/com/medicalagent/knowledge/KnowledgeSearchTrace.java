package com.medicalagent.knowledge;

import java.util.List;

public record KnowledgeSearchTrace(
        List<KnowledgeChunkMatch> matches,
        long embeddingMillis,
        long retrievalMillis
) {
}
