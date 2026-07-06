package com.medicalagent.prompt;

import com.medicalagent.api.AgentRequest;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContext;
import com.medicalagent.context.AgentContextFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptRendererTest {

    @Test
    void shouldRenderPromptWithAgentContextVariables() {
        AppConfig config = new ConfigLoader().load("test");
        AgentContext context = new AgentContextFactory(config).create(new AgentRequest(
                "Headache for two days",
                "session-9",
                "user-9",
                List.of(),
                "Recurring headache symptoms",
                JsonSupport.NODE_FACTORY.objectNode().put("triage", "observation"),
                false,
                Map.of("channel", "api")
        ));

        PromptTemplate template = new FilePromptLoader(config.getPrompt()).load(config.getPrompt().getDefaultTemplate());
        String rendered = new PromptRenderer(config.getPrompt()).render(template, new PromptVariablesFactory().create(context));

        assertTrue(rendered.contains("Headache for two days"));
        assertTrue(rendered.contains("Recurring headache symptoms"));
        assertTrue(rendered.contains("triage"));
        assertFalse(rendered.contains("{{message}}"));
    }
}
