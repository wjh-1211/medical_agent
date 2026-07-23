package com.medicalagent.knowledge;

import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldBuildLangChain4jChunksAndRetrieveControlledEvidence() {
        AppConfig config = new ConfigLoader().load("test");
        config.getKnowledge().setIndexSqlitePath(temporaryDirectory.resolve("knowledge.db").toString());
        KnowledgeService service = new KnowledgeServiceFactory().create(config.getKnowledge()).orElseThrow();

        service.rebuildIndex();
        var matches = service.search("呼吸困难时怎么办", 2);

        assertEquals(2, service.indexedChunkCount());
        assertFalse(matches.isEmpty());
        assertEquals("RAG Test Fixture", matches.get(0).chunk().source());
        assertTrue(matches.get(0).chunk().content().contains("呼吸困难"));
        assertTrue(matches.get(0).chunk().chunkId().matches("[0-9a-f]{24}"));
        service.close();
    }

    @Test
    void shouldUseHybridRetrievalAndRejectNoLexicalEvidence() {
        AppConfig config = new ConfigLoader().load("test");
        config.getKnowledge().setIndexSqlitePath(temporaryDirectory.resolve("hybrid-knowledge.db").toString());
        config.getKnowledge().setRetrievalStrategy("hybrid");
        KnowledgeService service = new KnowledgeServiceFactory().create(config.getKnowledge()).orElseThrow();

        service.rebuildIndex();
        var trace = service.searchWithTrace("呼吸困难", 2);

        assertEquals("hybrid", trace.strategy());
        assertFalse(trace.matches().isEmpty());
        assertTrue(service.search("zxqv-never-in-corpus", 2).isEmpty());
        service.close();
    }
}
