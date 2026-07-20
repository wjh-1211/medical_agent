package com.medicalagent.knowledge;

import java.util.List;

public interface KnowledgeRetriever extends AutoCloseable {

    List<KnowledgeChunkMatch> search(String query, int topK);

    @Override
    default void close() {
    }
}
