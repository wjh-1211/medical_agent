package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;

public interface Skill {

    String id();

    ToolSpec tool();

    JsonNode execute(JsonNode input);
}
