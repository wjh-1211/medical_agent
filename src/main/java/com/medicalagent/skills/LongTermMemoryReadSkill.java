package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.memory.LongTermMemoryRecord;
import com.medicalagent.memory.LongTermMemoryStore;

import java.util.List;

public class LongTermMemoryReadSkill implements Skill {

    public static final String TOOL_NAME = "long_term_memory_read";

    private final LongTermMemoryStore longTermMemoryStore;

    public LongTermMemoryReadSkill(LongTermMemoryStore longTermMemoryStore) {
        this.longTermMemoryStore = longTermMemoryStore;
    }

    @Override
    public String id() {
        return "longTermMemoryReadSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("userId").put("type", "string");
        schema.putArray("required").add("userId");
        return new ToolSchema(TOOL_NAME, "Read confirmed long-term facts for one user.", schema);
    }

    @Override
    public JsonNode execute(JsonNode input) {
        String userId = input.path("userId").asText();
        List<LongTermMemoryRecord> records = longTermMemoryStore.read(userId);
        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("status", "ok");
        output.put("userId", userId);
        output.put("found", !records.isEmpty());
        ArrayNode serializedRecords = output.putArray("records");
        records.forEach(record -> serializedRecords.add(serialize(record)));
        return output;
    }

    private ObjectNode serialize(LongTermMemoryRecord record) {
        ObjectNode node = JsonSupport.NODE_FACTORY.objectNode();
        node.put("userId", record.userId());
        node.put("category", record.category().wireValue());
        node.put("fact", record.fact());
        node.put("source", record.source());
        node.put("createdAt", record.createdAt().toString());
        node.put("updatedAt", record.updatedAt().toString());
        return node;
    }
}
