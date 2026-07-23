package com.medicalagent.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridRetrievalTest {

    @Test
    void shouldRankLexicalEvidenceAndFuseWithoutDuplicateChunks() {
        KnowledgeChunk breathing = chunk("a", "呼吸困难需要紧急医疗帮助");
        KnowledgeChunk fever = chunk("b", "持续高热需要线下评估");
        List<KnowledgeChunkMatch> lexical = new Bm25KnowledgeIndex(List.of(breathing, fever)).search("呼吸困难", 2);
        List<KnowledgeChunkMatch> fused = new ReciprocalRankFusion().fuse(
                List.of(new KnowledgeChunkMatch(fever, 0.9d), new KnowledgeChunkMatch(breathing, 0.8d)), lexical, 60
        );

        assertEquals("a", lexical.get(0).chunk().chunkId());
        assertEquals(2, fused.size());
        assertTrue(new KnowledgeReranker().rerank("呼吸困难", fused, 2).get(0).chunk().content().contains("呼吸困难"));
    }

    private KnowledgeChunk chunk(String id, String content) {
        return new KnowledgeChunk(id, content, "test", "test", "v1", id);
    }
}
