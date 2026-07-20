package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.memory.SummaryMemorySnapshot;
import com.medicalagent.memory.SummaryMemoryStore;

import java.util.Optional;

public class SummaryMemoryReadSkill implements Skill {

    public static final String TOOL_NAME = "summary_memory_read";

    private final SummaryMemoryStore summaryMemoryStore;

    public SummaryMemoryReadSkill(SummaryMemoryStore summaryMemoryStore) {
        this.summaryMemoryStore = summaryMemoryStore;
    }

    @Override
    public String id() {
        return "summaryMemoryReadSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("sessionId").put("type", "string");
        schema.putArray("required").add("sessionId");
        return new ToolSchema(TOOL_NAME, "Read compressed summary memory for one conversation session.", schema);
    }

    @Override
    public JsonNode execute(JsonNode input) {
        String sessionId = input.path("sessionId").asText();
        Optional<SummaryMemorySnapshot> snapshot = summaryMemoryStore.read(sessionId);
        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("status", "ok");
        output.put("sessionId", sessionId);
        output.put("found", snapshot.isPresent());
        snapshot.ifPresent(value -> {
            output.put("summary", value.summary());
            output.put("updatedAt", value.updatedAt().toString());
        });
        return output;
    }
}
