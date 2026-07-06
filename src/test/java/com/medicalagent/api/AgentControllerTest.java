package com.medicalagent.api;

import com.medicalagent.agent.AgentKernel;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentControllerTest {

    @Test
    void shouldRejectBlankMessage() {
        AppConfig config = new ConfigLoader().load("test");
        AgentController controller = new AgentController(
                new AgentContextFactory(config),
                createKernel(config)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.handle(new AgentRequest("  ", null, null, List.of(), null, null, null, Map.of()))
        );

        assertEquals("request.message must not be blank", exception.getMessage());
    }

    @Test
    void shouldBuildContextAndReturnKernelResponse() {
        AppConfig config = new ConfigLoader().load("test");
        AgentController controller = new AgentController(
                new AgentContextFactory(config),
                createKernel(config)
        );

        AgentResponse response = controller.handle(new AgentRequest(
                "Need help",
                "session-42",
                "user-7",
                List.of(new AgentMessage("user", "Need help")),
                null,
                null,
                null,
                Map.of("channel", "api")
        ));

        assertEquals("ok", response.status());
        assertEquals("session-42", response.sessionId());
        assertEquals("user-7", response.userId());
        assertNotNull(response.requestId());
        assertNotNull(response.createdAt());
        assertNotNull(response.answer());
    }

    private AgentKernel createKernel(AppConfig config) {
        SkillRegistry registry = new SkillRegistry(config, List.of(new EchoSkill()));
        return new AgentKernel(
                config,
                new ToolRouter(registry),
                new FilePromptLoader(config.getPrompt()),
                new PromptVariablesFactory(),
                new PromptRenderer(config.getPrompt()),
                new LocalModelGatewayRegistry(List.of(
                        new StubLocalModelGatewayFactory(),
                        new PythonTransformersLocalModelGatewayFactory()
                )).create(config)
        );
    }
}
