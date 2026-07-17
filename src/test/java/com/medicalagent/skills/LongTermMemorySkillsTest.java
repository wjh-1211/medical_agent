package com.medicalagent.skills;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.memory.SqliteLongTermMemoryStore;
import com.medicalagent.runtime.ToolRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongTermMemorySkillsTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldWriteAndReadLongTermMemoryThroughToolRouter() {
        AppConfig config = new ConfigLoader().load("test");
        SqliteLongTermMemoryStore store = new SqliteLongTermMemoryStore(temporaryDirectory.resolve("skills.db"));
        SkillRegistry registry = new SkillRegistry(config, List.of(
                new LongTermMemoryReadSkill(store),
                new LongTermMemoryWriteSkill(store)
        ));
        ToolRouter router = new ToolRouter(registry);

        ObjectNode writeInput = JsonSupport.NODE_FACTORY.objectNode();
        writeInput.put("userId", "user-1");
        writeInput.put("category", "allergy");
        writeInput.put("fact", "Allergic to penicillin");
        writeInput.put("source", "user_confirmed");
        ObjectNode readInput = JsonSupport.NODE_FACTORY.objectNode();
        readInput.put("userId", "user-1");

        assertEquals("ok", router.route(LongTermMemoryWriteSkill.TOOL_NAME, writeInput).path("status").asText());
        var readResult = router.route(LongTermMemoryReadSkill.TOOL_NAME, readInput);

        assertTrue(readResult.path("found").asBoolean());
        assertEquals("allergy", readResult.path("records").get(0).path("category").asText());
        assertEquals("Allergic to penicillin", readResult.path("records").get(0).path("fact").asText());
    }
}
