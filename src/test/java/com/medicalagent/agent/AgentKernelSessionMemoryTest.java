package com.medicalagent.agent;

import com.medicalagent.api.AgentRequest;
import com.medicalagent.api.AgentResponse;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.memory.InMemorySessionMemoryStore;
import com.medicalagent.memory.SessionMemoryStore;
import com.medicalagent.model.LocalModelGatewayRegistry;
import com.medicalagent.model.PythonTransformersLocalModelGatewayFactory;
import com.medicalagent.model.StubLocalModelGatewayFactory;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.MemoryReadSkill;
import com.medicalagent.skills.MemoryWriteSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.UppercaseSkill;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentKernelSessionMemoryTest {

    @Test
    void shouldRecallSessionMemoryOnLaterTurn() {
        AppConfig config = new ConfigLoader().load("test");
        SessionMemoryStore store = new InMemorySessionMemoryStore(Duration.ofMinutes(config.getMemory().getSessionTtlMinutes()));
        SkillRegistry registry = new SkillRegistry(config, List.of(
                new EchoSkill(),
                new UppercaseSkill(),
                new MemoryReadSkill(store),
                new MemoryWriteSkill(store)
        ));
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
        AgentContextFactory contextFactory = new AgentContextFactory(config);

        kernel.handle(contextFactory.create(new AgentRequest(
                "I am allergic to penicillin",
                "session-memory",
                "user-1",
                List.of(),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "test")
        )));

        AgentResponse secondResponse = kernel.handle(contextFactory.create(new AgentRequest(
                "What did I mention earlier?",
                "session-memory",
                "user-1",
                List.of(),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "test")
        )));

        assertTrue(secondResponse.answer().contains("Recent session notes:"));
        assertTrue(secondResponse.answer().contains("penicillin"));
    }
}
