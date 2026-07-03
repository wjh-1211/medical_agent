package com.medicalagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.skills.Skill;
import com.medicalagent.skills.SkillRegistry;

public class ToolRouter {

    private final SkillRegistry skillRegistry;

    public ToolRouter(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    public JsonNode route(String toolName, JsonNode input) {
        Skill skill = skillRegistry.findByToolName(toolName)
                .orElseThrow(() -> new IllegalArgumentException("No skill registered for tool: " + toolName));
        return skill.execute(input);
    }
}
