package com.medicalagent.agent;

import com.medicalagent.api.AgentRequest;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.knowledge.KnowledgeChunk;
import com.medicalagent.knowledge.KnowledgeChunkMatch;
import com.medicalagent.knowledge.KnowledgeRetriever;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.KnowledgeSearchSkill;
import com.medicalagent.skills.SkillRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentKernelKnowledgeSearchTest {

    @Test
    void shouldFeedKnowledgeToolEvidenceBackIntoReActObservation() {
        AppConfig config = new ConfigLoader().load("test");
        KnowledgeRetriever retriever = (query, topK) -> List.of(new KnowledgeChunkMatch(
                new KnowledgeChunk(
                        "a1b2c3d4e5f60718293a4b5c",
                        "出现呼吸困难时需要尽快寻求紧急医疗帮助。",
                        "RAG Test Fixture",
                        "呼吸困难",
                        "test-v1",
                        "hash"
                ),
                0.91d
        ));
        SkillRegistry registry = new SkillRegistry(config, List.of(
                new KnowledgeSearchSkill(retriever, 2)
        ));
        SequenceGateway gateway = new SequenceGateway(config, List.of(
                "{\"type\":\"tool_call\",\"toolName\":\"knowledge_search\",\"input\":{\"query\":\"呼吸困难\"}}",
                "{\"type\":\"final_answer\",\"answer\":\"出现呼吸困难时应尽快寻求紧急医疗帮助。\"}"
        ));
        AgentKernel kernel = new AgentKernel(
                config,
                registry,
                new ToolRouter(registry),
                new FilePromptLoader(config.getPrompt()),
                new PromptVariablesFactory(config.getContext()),
                new PromptRenderer(config.getPrompt()),
                gateway,
                new AgentDecisionParser()
        );

        var response = kernel.handle(new AgentContextFactory(config).create(new AgentRequest(
                "受控知识库中，呼吸困难有什么提示？",
                "session-1",
                "user-1",
                List.of(),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "test")
        )));

        assertTrue(response.answer().contains("[source: RAG Test Fixture | chunk: a1b2c3d4e5f60718293a4b5c]"));
        assertEquals(2, gateway.prompts.size());
        assertTrue(gateway.prompts.get(1).contains("RAG Test Fixture"));
        assertTrue(gateway.prompts.get(1).contains("a1b2c3d4e5f60718293a4b5c"));
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
            return "knowledge-search-test";
        }

        @Override
        public com.medicalagent.config.ModelConfig descriptor() {
            return config.getModel();
        }

        @Override
        public LocalModelResponse generate(LocalModelRequest request) {
            prompts.add(request.prompt());
            String content = responses.remove();
            return new LocalModelResponse(content, request.modelName(), provider(), 0L, request.prompt().length(), content.length());
        }
    }
}
