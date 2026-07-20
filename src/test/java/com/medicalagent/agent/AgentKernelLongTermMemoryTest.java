package com.medicalagent.agent;

import com.medicalagent.api.AgentRequest;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.memory.InMemorySessionMemoryStore;
import com.medicalagent.memory.LongTermMemoryStore;
import com.medicalagent.memory.SessionMemoryStore;
import com.medicalagent.memory.SqliteLongTermMemoryStore;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.LongTermMemoryReadSkill;
import com.medicalagent.skills.LongTermMemoryWriteSkill;
import com.medicalagent.skills.MemoryReadSkill;
import com.medicalagent.skills.MemoryWriteSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.UppercaseSkill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentKernelLongTermMemoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldWriteConfirmedFactThenRecallItInANewSession() {
        AppConfig config = new ConfigLoader().load("test");
        SessionMemoryStore sessionStore = new InMemorySessionMemoryStore(
                Duration.ofMinutes(config.getMemory().getSessionTtlMinutes())
        );
        LongTermMemoryStore longTermStore = new SqliteLongTermMemoryStore(temporaryDirectory.resolve("agent.db"));
        SkillRegistry registry = registry(config, sessionStore, longTermStore);
        AgentContextFactory contextFactory = new AgentContextFactory(config);

        SequenceGateway writingGateway = new SequenceGateway(config, List.of(
                "{\"type\":\"final_answer\",\"answer\":\"I will keep that confirmed allergy for future sessions.\"}",
                """
                        {"type":"tool_call","toolName":"long_term_memory_write","input":{
                          "userId":"user-1",
                          "category":"allergy",
                          "fact":"Allergic to penicillin",
                          "source":"user_confirmed"
                        }}
                        """
        ));
        AgentKernel writingKernel = kernel(config, registry, writingGateway);
        writingKernel.handle(contextFactory.create(new AgentRequest(
                "Please remember that I am allergic to penicillin.",
                "session-1",
                "user-1",
                List.of(),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "test")
        )));

        SequenceGateway recallingGateway = new SequenceGateway(config, List.of(
                "{\"type\":\"final_answer\",\"answer\":\"Your confirmed allergy is available.\"}",
                "{\"type\":\"final_answer\",\"answer\":\"skip\"}"
        ));
        AgentKernel recallingKernel = kernel(config, registry, recallingGateway);
        var response = recallingKernel.handle(contextFactory.create(new AgentRequest(
                "What allergy should be considered?",
                "session-2",
                "user-1",
                List.of(),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "test")
        )));

        assertEquals("Your confirmed allergy is available.", response.answer());
        assertTrue(recallingGateway.prompts.get(0).contains("Long-term user facts: allergy: Allergic to penicillin"));
        assertTrue(recallingGateway.prompts.get(0).contains("Long-term Memory:"));
    }

    @Test
    void shouldNotWriteLongTermMemoryWhenModelAnswersGreetingDirectly() {
        AppConfig config = new ConfigLoader().load("test");
        SessionMemoryStore sessionStore = new InMemorySessionMemoryStore(
                Duration.ofMinutes(config.getMemory().getSessionTtlMinutes())
        );
        LongTermMemoryStore longTermStore = new SqliteLongTermMemoryStore(temporaryDirectory.resolve("greeting.db"));
        SkillRegistry registry = registry(config, sessionStore, longTermStore);
        AgentKernel kernel = kernel(config, registry, new SequenceGateway(config, List.of(
                "{\"type\":\"final_answer\",\"answer\":\"Hello.\"}",
                "{\"type\":\"final_answer\",\"answer\":\"skip\"}"
        )));

        kernel.handle(new AgentContextFactory(config).create(new AgentRequest(
                "Hello",
                "session-1",
                "user-2",
                List.of(),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "test")
        )));

        assertTrue(longTermStore.read("user-2").isEmpty());
    }

    private SkillRegistry registry(
            AppConfig config,
            SessionMemoryStore sessionStore,
            LongTermMemoryStore longTermStore
    ) {
        return new SkillRegistry(config, List.of(
                new EchoSkill(),
                new UppercaseSkill(),
                new MemoryReadSkill(sessionStore),
                new MemoryWriteSkill(sessionStore),
                new LongTermMemoryReadSkill(longTermStore),
                new LongTermMemoryWriteSkill(longTermStore)
        ));
    }

    private AgentKernel kernel(AppConfig config, SkillRegistry registry, LocalModelGateway gateway) {
        return new AgentKernel(
                config,
                registry,
                new ToolRouter(registry),
                new FilePromptLoader(config.getPrompt()),
                new PromptVariablesFactory(),
                new PromptRenderer(config.getPrompt()),
                gateway,
                new AgentDecisionParser()
        );
    }

    private static final class SequenceGateway implements LocalModelGateway {

        private final AppConfig config;
        private final Queue<String> responses;
        private final List<String> prompts = new ArrayList<>();

        private SequenceGateway(AppConfig config, List<String> responses) {
            this.config = config;
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public String provider() {
            return "sequence-long-term-memory-test";
        }

        @Override
        public com.medicalagent.config.ModelConfig descriptor() {
            return config.getModel();
        }

        @Override
        public LocalModelResponse generate(LocalModelRequest request) {
            prompts.add(request.prompt());
            String content = responses.remove();
            return new LocalModelResponse(
                    content,
                    request.modelName(),
                    provider(),
                    0L,
                    request.prompt().length(),
                    content.length()
            );
        }
    }
}
