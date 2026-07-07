package com.medicalagent.runtime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.UppercaseSkill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolRouterTest {

    @Test
    void shouldRejectMissingRequiredField() {
        AppConfig config = new ConfigLoader().load("test");
        ToolRouter router = new ToolRouter(new SkillRegistry(config, List.of(new EchoSkill(), new UppercaseSkill())));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> router.route("uppercase_text", JsonSupport.NODE_FACTORY.objectNode())
        );

        assertEquals("Missing required field 'message' for tool: uppercase_text", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidFieldType() {
        AppConfig config = new ConfigLoader().load("test");
        ToolRouter router = new ToolRouter(new SkillRegistry(config, List.of(new EchoSkill(), new UppercaseSkill())));
        ObjectNode input = JsonSupport.NODE_FACTORY.objectNode();
        input.put("message", 123);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> router.route("uppercase_text", input)
        );

        assertEquals("Invalid field type for 'message' on tool uppercase_text: expected string", exception.getMessage());
    }

    @Test
    void shouldRejectUnknownToolName() {
        AppConfig config = new ConfigLoader().load("test");
        ToolRouter router = new ToolRouter(new SkillRegistry(config, List.of(new EchoSkill(), new UppercaseSkill())));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> router.route("unknown_tool", JsonSupport.NODE_FACTORY.objectNode())
        );

        assertEquals("No skill registered for tool: unknown_tool", exception.getMessage());
    }
}
