package com.medicalagent.skills;

import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.config.SkillConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolSchemaRegistryTest {

    @Test
    void shouldRejectDuplicateToolRegistrations() {
        AppConfig config = new ConfigLoader().load("test");
        SkillConfig uppercaseConfig = config.getSkills().get("uppercaseSkill");
        uppercaseConfig.setToolName("echo_input");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SkillRegistry(config, List.of(new EchoSkill(), new UppercaseSkill()))
        );

        assertEquals("Duplicate tool registration for tool: echo_input", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidToolSchemaDefinition() {
        Skill invalidSkill = new Skill() {
            @Override
            public String id() {
                return "invalidSkill";
            }

            @Override
            public ToolSchema toolSchema() {
                return new ToolSchema("invalid_tool", "", JsonSupport.NODE_FACTORY.textNode("bad"));
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode execute(com.fasterxml.jackson.databind.JsonNode input) {
                return input;
            }
        };

        SkillConfig config = new SkillConfig();
        config.setEnabled(true);
        config.setToolName("invalid_tool");
        SkillRegistration registration = new SkillRegistration(invalidSkill, invalidSkill.toolSchema(), config);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ToolSchemaRegistry(List.of(registration))
        );

        assertEquals("Tool schema description must not be blank for tool: invalid_tool", exception.getMessage());
    }
}
