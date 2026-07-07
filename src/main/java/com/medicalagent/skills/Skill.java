package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;

public interface Skill {

    String id();

    ToolSchema toolSchema();

    JsonNode execute(JsonNode input);
}
