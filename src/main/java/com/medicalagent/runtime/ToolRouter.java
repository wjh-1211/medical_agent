package com.medicalagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.SkillRegistration;
import com.medicalagent.skills.ToolSchema;

public class ToolRouter {

    private final SkillRegistry skillRegistry;
    private final ToolInputValidator toolInputValidator;

    public ToolRouter(SkillRegistry skillRegistry) {
        this(skillRegistry, new ToolInputValidator());
    }

    public ToolRouter(SkillRegistry skillRegistry, ToolInputValidator toolInputValidator) {
        this.skillRegistry = skillRegistry;
        this.toolInputValidator = toolInputValidator;
    }

    public JsonNode route(String toolName, JsonNode input) {
        SkillRegistration registration = skillRegistry.findRegistrationByToolName(toolName)
                .orElseThrow(() -> new IllegalArgumentException("No skill registered for tool: " + toolName));
        ToolSchema schema = skillRegistry.findSchemaByToolName(toolName)
                .orElseThrow(() -> new IllegalStateException("No tool schema registered for tool: " + toolName));
        toolInputValidator.validate(schema, input);
        return registration.skill().execute(input);
    }
}
