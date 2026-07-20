package com.medicalagent.knowledge;

public record KnowledgeChunk(
        String chunkId,
        String content,
        String source,
        String section,
        String documentVersion,
        String contentHash
) {
}
