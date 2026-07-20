package com.medicalagent.skills;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.memory.SqliteSummaryMemoryStore;
import com.medicalagent.runtime.ToolRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryMemorySkillsTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldWriteAndReadSummaryThroughToolRouter() {
        AppConfig config = new ConfigLoader().load("test");
        SqliteSummaryMemoryStore store = new SqliteSummaryMemoryStore(temporaryDirectory.resolve("skills.db"));
        SkillRegistry registry = new SkillRegistry(config, List.of(
                new SummaryMemoryReadSkill(store),
                new SummaryMemoryWriteSkill(store)
        ));
        ToolRouter router = new ToolRouter(registry);
        ObjectNode writeInput = JsonSupport.NODE_FACTORY.objectNode();
        writeInput.put("sessionId", "session-1");
        writeInput.put("summary", "Confirmed fever reached 38.5C.");
        ObjectNode readInput = JsonSupport.NODE_FACTORY.objectNode();
        readInput.put("sessionId", "session-1");

        assertEquals("ok", router.route(SummaryMemoryWriteSkill.TOOL_NAME, writeInput).path("status").asText());
        var readResult = router.route(SummaryMemoryReadSkill.TOOL_NAME, readInput);

        assertTrue(readResult.path("found").asBoolean());
        assertEquals("Confirmed fever reached 38.5C.", readResult.path("summary").asText());
    }
}
