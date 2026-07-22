package com.medicalagent.agent;

import com.medicalagent.api.AgentRequest;
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
import com.medicalagent.skills.EmergencyDetectionSkill;
import com.medicalagent.skills.GroundingCheckSkill;
import com.medicalagent.skills.KnowledgeSearchSkill;
import com.medicalagent.skills.RiskAssessmentSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.tracing.InMemoryTraceSink;
import com.medicalagent.tracing.TraceEventType;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentKernelSwarmTest {

    @Test
    void shouldUseSwarmEvidenceThenRunExistingGuardrail() {
        AppConfig config = new ConfigLoader().load("test");
        config.getSwarm().setEnabled(true);
        InMemoryTraceSink traceSink = new InMemoryTraceSink();
        SkillRegistry registry = registry(config);
        SequenceGateway gateway = new SequenceGateway(config, List.of(
                "{\"mode\":\"swarm\",\"tasks\":[{\"role\":\"retriever\",\"query\":\"呼吸困难\"},{\"role\":\"safety\",\"query\":\"\"}]}",
                "{\"type\":\"final_answer\",\"answer\":\"受控资料提示出现呼吸困难应及时评估。\"}",
                "{\"emergency\":false,\"urgency\":\"none\",\"reason\":\"fixture non-emergency\"}",
                "{\"riskLevel\":\"low\",\"requiresFollowUp\":false,\"reason\":\"fixture low risk\"}",
                "{\"requiresEvidence\":false,\"reason\":\"fixture citation checked\"}"
        ));
        AgentKernel kernel = new AgentKernel(
                config, registry, new ToolRouter(registry, traceSink, 320, 1),
                new FilePromptLoader(config.getPrompt()), new PromptVariablesFactory(config.getContext()),
                new PromptRenderer(config.getPrompt()), gateway, new AgentDecisionParser(), traceSink
        );
        var context = new AgentContextFactory(config).create(new AgentRequest(
                "请结合受控知识说明呼吸困难提示", "kernel-swarm-session", "kernel-swarm-user", List.of(), null, null, false,
                Map.of("channel", "test")
        ));

        var response = kernel.handle(context);

        assertTrue(response.answer().contains("[source: Swarm Kernel Fixture | chunk: a1b2c3d4e5f60718293a4b5c]"));
        assertEquals(5, gateway.prompts.size());
        assertTrue(gateway.prompts.get(1).contains("Swarm Kernel Fixture"));
        var events = traceSink.findByTraceId(response.traceId());
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.SWARM_PLAN));
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.SWARM_ROLE));
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.SWARM_MERGE));
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.GUARDRAIL_ACTION));
        assertTrue(events.stream().allMatch(event -> response.traceId().equals(event.traceId())));
    }

    private SkillRegistry registry(AppConfig config) {
        KnowledgeRetriever retriever = (query, topK) -> List.of(new KnowledgeChunkMatch(new KnowledgeChunk(
                "a1b2c3d4e5f60718293a4b5c", "出现呼吸困难应及时线下评估。", "Swarm Kernel Fixture", "提示", "test-v1", "hash"), 0.9d));
        return new SkillRegistry(config, List.of(
                new KnowledgeSearchSkill(retriever, 2),
                new EmergencyDetectionSkill(),
                new RiskAssessmentSkill(),
                new GroundingCheckSkill()
        ));
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
            return "kernel-swarm-test";
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
