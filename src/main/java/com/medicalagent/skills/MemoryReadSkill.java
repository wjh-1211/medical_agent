package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.memory.SessionMemorySnapshot;
import com.medicalagent.memory.SessionMemoryStore;

import java.util.Optional;

public class MemoryReadSkill implements Skill {

    public static final String TOOL_NAME = "session_memory_read";

    private final SessionMemoryStore sessionMemoryStore;

    public MemoryReadSkill(SessionMemoryStore sessionMemoryStore) {
        this.sessionMemoryStore = sessionMemoryStore;
    }

    @Override
    public String id() {
        return "memoryReadSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("sessionId").put("type", "string");
        schema.putArray("required").add("sessionId");
        return new ToolSchema(
                TOOL_NAME,
                "Read short-term session memory for the current conversation session.",
                schema
        );
    }

    @Override
    public JsonNode execute(JsonNode input) {
        String sessionId = input.path("sessionId").asText();
        Optional<SessionMemorySnapshot> snapshot = sessionMemoryStore.read(sessionId);

        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("status", "ok");
        output.put("sessionId", sessionId);
        output.put("found", snapshot.isPresent());
        if (snapshot.isPresent()) {
            SessionMemorySnapshot record = snapshot.get();
            output.put("memorySummary", record.memorySummary());
            output.put("lastUserMessage", record.lastUserMessage());
            output.put("lastAgentAnswer", record.lastAgentAnswer());
            output.put("updatedAt", record.updatedAt().toString());
            output.put("expiresAt", record.expiresAt().toString());
        }
        return output;
    }
}
