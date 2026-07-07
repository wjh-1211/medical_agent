package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;

public record ToolSchema(
        String name,
        String description,
        JsonNode inputSchema
) {

    public ToolSchema {
        inputSchema = inputSchema == null ? JsonSupport.NODE_FACTORY.objectNode() : inputSchema.deepCopy();
    }

    public ToolSchema withName(String overriddenName) {
        return new ToolSchema(overriddenName, description, inputSchema);
    }
}
