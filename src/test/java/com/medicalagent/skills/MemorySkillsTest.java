package com.medicalagent.skills;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.memory.InMemorySessionMemoryStore;
import com.medicalagent.memory.SessionMemoryStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemorySkillsTest {

    @Test
    void shouldWriteAndReadSessionMemoryThroughSkills() {
        SessionMemoryStore store = new InMemorySessionMemoryStore(Duration.ofMinutes(30));
        MemoryWriteSkill writeSkill = new MemoryWriteSkill(store);
        MemoryReadSkill readSkill = new MemoryReadSkill(store);

        ObjectNode writeInput = JsonSupport.NODE_FACTORY.objectNode();
        writeInput.put("sessionId", "session-1");
        writeInput.put("userMessage", "I am allergic to penicillin");
        writeInput.put("agentAnswer", "I will remember that.");

        ObjectNode readInput = JsonSupport.NODE_FACTORY.objectNode();
        readInput.put("sessionId", "session-1");

        assertEquals("ok", writeSkill.execute(writeInput).path("status").asText());
        assertTrue(readSkill.execute(readInput).path("found").asBoolean());
        assertTrue(readSkill.execute(readInput).path("memorySummary").asText().contains("penicillin"));
    }
}
