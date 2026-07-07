package com.medicalagent.skills;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.runtime.ToolRouter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillRegistryTest {

    @Test
    void shouldRegisterConfiguredSkillsAndExposeSchemas() {
        AppConfig config = new ConfigLoader().load("test");
        SkillRegistry registry = new SkillRegistry(config, List.of(new EchoSkill(), new UppercaseSkill()));
        ToolRouter router = new ToolRouter(registry);

        assertTrue(registry.findByToolName("echo_input").isPresent());
        assertTrue(registry.findSchemaByToolName("uppercase_text").isPresent());

        ObjectNode input = JsonSupport.NODE_FACTORY.objectNode();
        input.put("message", "hello");

        assertEquals("ok", router.route("echo_input", input).get("status").asText());
        assertEquals("HELLO", router.route("uppercase_text", input).get("transformed").asText());
    }
}
