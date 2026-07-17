package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.memory.LongTermMemoryCategory;
import com.medicalagent.memory.LongTermMemoryRecord;
import com.medicalagent.memory.LongTermMemoryStore;

public class LongTermMemoryWriteSkill implements Skill {

    public static final String TOOL_NAME = "long_term_memory_write";

    private final LongTermMemoryStore longTermMemoryStore;

    public LongTermMemoryWriteSkill(LongTermMemoryStore longTermMemoryStore) {
        this.longTermMemoryStore = longTermMemoryStore;
    }

    @Override
    public String id() {
        return "longTermMemoryWriteSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("userId").put("type", "string");
        properties.putObject("category").put("type", "string");
        properties.putObject("fact").put("type", "string");
        properties.putObject("source").put("type", "string");
        schema.putArray("required").add("userId").add("category").add("fact").add("source");
        return new ToolSchema(
                TOOL_NAME,
                "Persist one confirmed long-term user fact. Categories: medical_history, allergy, long_term_medication.",
                schema
        );
    }

    @Override
    public JsonNode execute(JsonNode input) {
        LongTermMemoryRecord record = longTermMemoryStore.upsert(
                input.path("userId").asText(),
                LongTermMemoryCategory.fromWireValue(input.path("category").asText()),
                input.path("fact").asText(),
                input.path("source").asText()
        );
        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("status", "ok");
        output.set("record", serialize(record));
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
