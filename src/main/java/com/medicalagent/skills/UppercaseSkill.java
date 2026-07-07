package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;

public class UppercaseSkill implements Skill {

    @Override
    public String id() {
        return "uppercaseSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("message").put("type", "string");
        schema.putArray("required").add("message");
        return new ToolSchema(
                "uppercase_text",
                "Convert the incoming message to uppercase as a deterministic sample tool.",
                schema
        );
    }

    @Override
    public JsonNode execute(JsonNode input) {
        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("skill", id());
        output.put("status", "ok");
        output.put("original", input.path("message").asText());
        output.put("transformed", input.path("message").asText().toUpperCase());
        return output;
    }
}
