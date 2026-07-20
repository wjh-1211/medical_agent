package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.memory.SummaryMemorySnapshot;
import com.medicalagent.memory.SummaryMemoryStore;

public class SummaryMemoryWriteSkill implements Skill {

    public static final String TOOL_NAME = "summary_memory_write";

    private final SummaryMemoryStore summaryMemoryStore;

    public SummaryMemoryWriteSkill(SummaryMemoryStore summaryMemoryStore) {
        this.summaryMemoryStore = summaryMemoryStore;
    }

    @Override
    public String id() {
        return "summaryMemoryWriteSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("sessionId").put("type", "string");
        properties.putObject("summary").put("type", "string");
        schema.putArray("required").add("sessionId").add("summary");
        return new ToolSchema(TOOL_NAME, "Write compressed summary memory for one conversation session.", schema);
    }

    @Override
    public JsonNode execute(JsonNode input) {
        SummaryMemorySnapshot snapshot = summaryMemoryStore.write(
                input.path("sessionId").asText(),
                input.path("summary").asText()
        );
        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("status", "ok");
        output.put("sessionId", snapshot.sessionId());
        output.put("summary", snapshot.summary());
        output.put("updatedAt", snapshot.updatedAt().toString());
        return output;
    }
}
