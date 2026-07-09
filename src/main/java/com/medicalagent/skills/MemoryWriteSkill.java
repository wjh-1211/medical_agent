package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.memory.SessionMemorySnapshot;
import com.medicalagent.memory.SessionMemoryStore;

public class MemoryWriteSkill implements Skill {

    public static final String TOOL_NAME = "session_memory_write";

    private final SessionMemoryStore sessionMemoryStore;

    public MemoryWriteSkill(SessionMemoryStore sessionMemoryStore) {
        this.sessionMemoryStore = sessionMemoryStore;
    }

    @Override
    public String id() {
        return "memoryWriteSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("sessionId").put("type", "string");
        properties.putObject("userMessage").put("type", "string");
        properties.putObject("agentAnswer").put("type", "string");
        schema.putArray("required").add("sessionId").add("userMessage").add("agentAnswer");
        return new ToolSchema(
                TOOL_NAME,
                "Write short-term session memory for the current conversation session.",
                schema
        );
    }

    @Override
    public JsonNode execute(JsonNode input) {
        SessionMemorySnapshot snapshot = sessionMemoryStore.write(
                input.path("sessionId").asText(),
                input.path("userMessage").asText(),
                input.path("agentAnswer").asText()
        );

        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("status", "ok");
        output.put("sessionId", snapshot.sessionId());
        output.put("memorySummary", snapshot.memorySummary());
        output.put("updatedAt", snapshot.updatedAt().toString());
        output.put("expiresAt", snapshot.expiresAt().toString());
        return output;
    }
}
