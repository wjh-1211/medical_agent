package com.medicalagent.agent;

import com.medicalagent.api.AgentRequest;
import com.medicalagent.api.AgentResponse;
import com.medicalagent.agent.AgentDecisionParser;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContext;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.model.LocalModelGatewayRegistry;
import com.medicalagent.model.PythonTransformersLocalModelGatewayFactory;
import com.medicalagent.model.StubLocalModelGatewayFactory;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.UppercaseSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentKernelTest {

    @Test
    void shouldLoadPromptCallToolAndReturnFinalAnswer() {
        AppConfig config = new ConfigLoader().load("test");
        SkillRegistry registry = new SkillRegistry(config, List.of(new EchoSkill(), new UppercaseSkill()));
        AgentKernel kernel = new AgentKernel(
                config,
                registry,
                new ToolRouter(registry),
                new FilePromptLoader(config.getPrompt()),
                new PromptVariablesFactory(),
                new PromptRenderer(config.getPrompt()),
                new LocalModelGatewayRegistry(List.of(
                        new StubLocalModelGatewayFactory(),
                        new PythonTransformersLocalModelGatewayFactory()
                )).create(config),
                new AgentDecisionParser()
        );
        AgentContext context = new AgentContextFactory(config).create(new AgentRequest(
                "Need advice for sore throat",
                "session-7",
                "user-7",
                List.of(),
                "Sore throat started yesterday",
                JsonSupport.NODE_FACTORY.objectNode().put("risk", "low"),
                false,
                Map.of("channel", "test")
        ));

        AgentResponse response = kernel.handle(context);

        assertEquals("ok", response.status());
        assertTrue(response.answer().contains("stub-final-answer:"));
        assertTrue(response.answer().contains("uppercase_text"));
    }
}
