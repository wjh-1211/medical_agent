package com.medicalagent.agent;

import com.medicalagent.api.AgentMessage;
import com.medicalagent.api.AgentRequest;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.memory.InMemorySessionMemoryStore;
import com.medicalagent.memory.SessionMemoryStore;
import com.medicalagent.memory.SqliteSummaryMemoryStore;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.MemoryReadSkill;
import com.medicalagent.skills.MemoryWriteSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.SummaryMemoryReadSkill;
import com.medicalagent.skills.SummaryMemoryWriteSkill;
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

class AgentKernelSummaryMemoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldWriteSummaryAfterThresholdThenRecallItOnLaterTurn() {
        AppConfig config = new ConfigLoader().load("test");
        SessionMemoryStore sessionStore = new InMemorySessionMemoryStore(
                Duration.ofMinutes(config.getMemory().getSessionTtlMinutes())
        );
        SqliteSummaryMemoryStore summaryStore = new SqliteSummaryMemoryStore(temporaryDirectory.resolve("summary.db"));
        SkillRegistry registry = registry(config, sessionStore, summaryStore);
        AgentContextFactory contextFactory = new AgentContextFactory(config);
        List<AgentMessage> qualifyingHistory = List.of(
                new AgentMessage("user", "original fever message that will be summarized"),
                new AgentMessage("assistant", "first response"),
                new AgentMessage("user", "symptoms worsened last night"),
                new AgentMessage("assistant", "second response")
        );

        SequenceGateway writingGateway = new SequenceGateway(config, List.of(
                "{\"type\":\"final_answer\",\"answer\":\"I will keep the session context concise.\"}",
                """
                        {"type":"tool_call","toolName":"summary_memory_write","input":{
                          "sessionId":"session-1",
                          "summary":"Confirmed session facts: fever reached 38.5C; symptoms worsened last night. Open questions: cough details."
                        }}
                        """
        ));
        kernel(config, registry, writingGateway).handle(contextFactory.create(new AgentRequest(
                "My temperature reached 38.5C.",
                "session-1",
                "user-1",
                qualifyingHistory,
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "test")
        )));

        SequenceGateway recallingGateway = new SequenceGateway(config, List.of(
                "{\"type\":\"final_answer\",\"answer\":\"The earlier fever context is available.\"}",
                "{\"type\":\"final_answer\",\"answer\":\"skip\"}"
        ));
        var response = kernel(config, registry, recallingGateway).handle(contextFactory.create(new AgentRequest(
                "What did I mention about the fever?",
                "session-1",
                "user-1",
                List.of(
                        new AgentMessage("user", "original-history-that-should-be-trimmed"),
                        new AgentMessage("assistant", "older response"),
                        new AgentMessage("user", "recent question"),
                        new AgentMessage("assistant", "recent response"),
                        new AgentMessage("user", "latest question")
                ),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "test")
        )));

        assertEquals("The earlier fever context is available.", response.answer());
        assertTrue(recallingGateway.prompts.get(0).contains("Summary Memory:"));
        assertTrue(recallingGateway.prompts.get(0).contains("fever reached 38.5C"));
        assertFalse(recallingGateway.prompts.get(0).contains("original-history-that-should-be-trimmed"));
    }

    @Test
    void shouldSkipSummaryUpdateBelowConfiguredHistoryThreshold() {
        AppConfig config = new ConfigLoader().load("test");
        SessionMemoryStore sessionStore = new InMemorySessionMemoryStore(
                Duration.ofMinutes(config.getMemory().getSessionTtlMinutes())
        );
        SqliteSummaryMemoryStore summaryStore = new SqliteSummaryMemoryStore(temporaryDirectory.resolve("skip.db"));
        SkillRegistry registry = registry(config, sessionStore, summaryStore);
        SequenceGateway gateway = new SequenceGateway(config, List.of(
                "{\"type\":\"final_answer\",\"answer\":\"Hello.\"}"
        ));

        kernel(config, registry, gateway).handle(new AgentContextFactory(config).create(new AgentRequest(
                "Hello",
                "session-1",
                "user-1",
                List.of(),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "test")
        )));

        assertEquals(1, gateway.prompts.size());
        assertTrue(summaryStore.read("session-1").isEmpty());
    }

    @Test
    void shouldNotWriteSummaryWhenModelSkipsAHighHistoryGreeting() {
        AppConfig config = new ConfigLoader().load("test");
        SessionMemoryStore sessionStore = new InMemorySessionMemoryStore(
                Duration.ofMinutes(config.getMemory().getSessionTtlMinutes())
        );
        SqliteSummaryMemoryStore summaryStore = new SqliteSummaryMemoryStore(temporaryDirectory.resolve("model-skip.db"));
        SkillRegistry registry = registry(config, sessionStore, summaryStore);
        SequenceGateway gateway = new SequenceGateway(config, List.of(
                "{\"type\":\"final_answer\",\"answer\":\"Hello.\"}",
                "{\"type\":\"final_answer\",\"answer\":\"skip\"}"
        ));

        kernel(config, registry, gateway).handle(new AgentContextFactory(config).create(new AgentRequest(
                "Hello again",
                "session-1",
                "user-1",
                List.of(
                        new AgentMessage("user", "hello"),
                        new AgentMessage("assistant", "hello"),
                        new AgentMessage("user", "thanks"),
                        new AgentMessage("assistant", "you are welcome")
                ),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "test")
        )));

        assertEquals(2, gateway.prompts.size());
        assertTrue(summaryStore.read("session-1").isEmpty());
    }

    private SkillRegistry registry(AppConfig config, SessionMemoryStore sessionStore, SqliteSummaryMemoryStore summaryStore) {
        return new SkillRegistry(config, List.of(
                new EchoSkill(),
                new UppercaseSkill(),
                new MemoryReadSkill(sessionStore),
                new MemoryWriteSkill(sessionStore),
                new SummaryMemoryReadSkill(summaryStore),
                new SummaryMemoryWriteSkill(summaryStore)
        ));
    }

    private AgentKernel kernel(AppConfig config, SkillRegistry registry, LocalModelGateway gateway) {
        return new AgentKernel(
                config,
                registry,
                new ToolRouter(registry),
                new FilePromptLoader(config.getPrompt()),
                new PromptVariablesFactory(config.getContext()),
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
            return "sequence-summary-memory-test";
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
