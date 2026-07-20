package com.medicalagent.skills;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.knowledge.KnowledgeService;
import com.medicalagent.knowledge.KnowledgeServiceFactory;
import com.medicalagent.knowledge.KnowledgeRetriever;
import com.medicalagent.runtime.ToolRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeSearchSkillTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldReturnSourceBearingEvidenceThroughToolRouter() {
        AppConfig config = new ConfigLoader().load("test");
        config.getKnowledge().setIndexSqlitePath(temporaryDirectory.resolve("knowledge.db").toString());
        KnowledgeService service = new KnowledgeServiceFactory().create(config.getKnowledge()).orElseThrow();
        service.rebuildIndex();
        SkillRegistry registry = new SkillRegistry(config, List.of(
                new KnowledgeSearchSkill(service, config.getKnowledge().getDefaultTopK())
        ));
        ObjectNode input = JsonSupport.NODE_FACTORY.objectNode();
        input.put("query", "嘴唇发青");
        input.put("topK", 1);

        var result = new ToolRouter(registry).route(KnowledgeSearchSkill.TOOL_NAME, input);

        assertEquals("ok", result.path("status").asText());
        assertTrue(result.path("found").asBoolean());
        assertEquals("RAG Test Fixture", result.path("chunks").get(0).path("source").asText());
        assertTrue(result.path("chunks").get(0).path("chunkId").asText().matches("[0-9a-f]{24}"));
        service.close();
    }

    @Test
    void shouldReturnFoundFalseWhenRetrieverHasNoEvidence() {
        AppConfig config = new ConfigLoader().load("test");
        KnowledgeRetriever noResultRetriever = (query, topK) -> List.of();
        SkillRegistry registry = new SkillRegistry(config, List.of(
                new KnowledgeSearchSkill(noResultRetriever, config.getKnowledge().getDefaultTopK())
        ));
        ObjectNode input = JsonSupport.NODE_FACTORY.objectNode();
        input.put("query", "不存在的受控资料");

        var result = new ToolRouter(registry).route(KnowledgeSearchSkill.TOOL_NAME, input);

        assertEquals("ok", result.path("status").asText());
        assertTrue(!result.path("found").asBoolean());
        assertEquals(0, result.path("chunks").size());
    }
}
