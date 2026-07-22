package com.medicalagent.swarm;

import com.medicalagent.api.AgentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContext;
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
import com.medicalagent.tracing.InMemoryTraceSink;
import com.medicalagent.tracing.TraceEventType;
import com.medicalagent.tracing.TraceScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwarmOrchestratorTest {

    @Test
    void shouldNotInvokePlannerWhenSwarmIsDisabled() {
        AppConfig config = new ConfigLoader().load("test");
        AtomicInteger modelCalls = new AtomicInteger();
        SwarmOrchestrator orchestrator = orchestrator(config, emptyRegistry(config), gateway(config, modelCalls,
                "{\"mode\":\"single_agent\",\"tasks\":[]}"), new InMemoryTraceSink());

        SwarmOutcome outcome = orchestrator.orchestrate(context(config));

        assertFalse(outcome.activated());
        assertEquals("swarm_disabled", outcome.fallbackReason());
        assertEquals(0, modelCalls.get());
    }

    @Test
    void shouldExecuteRetrieverAndSafetyWithOneTraceId() {
        AppConfig config = new ConfigLoader().load("test");
        config.getSwarm().setEnabled(true);
        InMemoryTraceSink traceSink = new InMemoryTraceSink();
        SkillRegistry registry = knowledgeRegistry(config);
        SwarmOrchestrator orchestrator = orchestrator(config, registry, gateway(config, new AtomicInteger(), """
                {"mode":"swarm","tasks":[
                  {"role":"retriever","query":"呼吸困难"},
                  {"role":"safety","query":""}
                ]}
                """), traceSink);
        AgentContext context = context(config);

        SwarmOutcome outcome;
        try (TraceScope ignored = TraceScope.open(context.getRequestId())) {
            outcome = orchestrator.orchestrate(context);
        }

        assertTrue(outcome.activated(), outcome.fallbackReason());
        assertEquals("", outcome.fallbackReason());
        assertEquals(2, outcome.roleResults().size());
        assertTrue(outcome.hasKnowledgeObservation());
        assertTrue(outcome.observations().get(outcome.observations().size() - 1).toString().contains("Swarm Fixture"));
        var events = traceSink.findByTraceId(context.getRequestId());
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.SWARM_PLAN));
        assertEquals(2, events.stream().filter(event -> event.type() == TraceEventType.SWARM_ROLE).count());
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.SWARM_MERGE));
        assertTrue(events.stream().allMatch(event -> context.getRequestId().equals(event.traceId())));
    }

    @Test
    void shouldFallbackWhenPlannerReturnsInvalidJson() {
        AppConfig config = new ConfigLoader().load("test");
        config.getSwarm().setEnabled(true);
        InMemoryTraceSink traceSink = new InMemoryTraceSink();
        AgentContext context = context(config);
        SwarmOrchestrator orchestrator = orchestrator(config, emptyRegistry(config), gateway(config, new AtomicInteger(), "not-json"), traceSink);

        SwarmOutcome outcome = orchestrator.orchestrate(context);

        assertFalse(outcome.activated());
        assertEquals("planner_failed", outcome.fallbackReason());
        assertTrue(traceSink.findByTraceId(context.getRequestId()).stream()
                .anyMatch(event -> event.type() == TraceEventType.SWARM_FALLBACK));
    }

    @Test
    void shouldFallbackWhenRoleCannotExecute() {
        AppConfig config = new ConfigLoader().load("test");
        config.getSwarm().setEnabled(true);
        InMemoryTraceSink traceSink = new InMemoryTraceSink();
        AgentContext context = context(config);
        SwarmOrchestrator orchestrator = orchestrator(config, emptyRegistry(config), gateway(config, new AtomicInteger(), retrieverSafetyPlan()), traceSink);

        SwarmOutcome outcome = orchestrator.orchestrate(context);

        assertFalse(outcome.activated());
        assertEquals("role_failed", outcome.fallbackReason());
        assertTrue(traceSink.findByTraceId(context.getRequestId()).stream()
                .anyMatch(event -> event.type() == TraceEventType.SWARM_FALLBACK));
    }

    @Test
    void shouldFallbackWhenRoleExceedsConfiguredTimeout() {
        AppConfig config = new ConfigLoader().load("test");
        config.getSwarm().setEnabled(true);
        config.getSwarm().setRoleTimeoutMillis(1);
        InMemoryTraceSink traceSink = new InMemoryTraceSink();
        KnowledgeSearchSkill slowSkill = new KnowledgeSearchSkill((query, topK) -> List.of(), 2) {
            @Override
            public JsonNode execute(JsonNode input) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted", exception);
                }
                return super.execute(input);
            }
        };
        SkillRegistry registry = new SkillRegistry(config, List.of(slowSkill));
        AgentContext context = context(config);
        SwarmOrchestrator orchestrator = orchestrator(config, registry, gateway(config, new AtomicInteger(), retrieverSafetyPlan()), traceSink);

        SwarmOutcome outcome = orchestrator.orchestrate(context);

        assertFalse(outcome.activated());
        assertEquals("role_timeout", outcome.fallbackReason());
    }

    private SwarmOrchestrator orchestrator(AppConfig config, SkillRegistry registry, LocalModelGateway gateway, InMemoryTraceSink traceSink) {
        return new SwarmOrchestrator(config, registry, new ToolRouter(registry, traceSink, 320, 1),
                new FilePromptLoader(config.getPrompt()), new PromptVariablesFactory(config.getContext()),
                new PromptRenderer(config.getPrompt()), gateway, traceSink);
    }

    private SkillRegistry emptyRegistry(AppConfig config) {
        return new SkillRegistry(config, List.of());
    }

    private SkillRegistry knowledgeRegistry(AppConfig config) {
        KnowledgeRetriever retriever = (query, topK) -> List.of(new KnowledgeChunkMatch(new KnowledgeChunk(
                "a1b2c3d4e5f60718293a4b5c", "出现呼吸困难应尽快线下评估。", "Swarm Fixture", "提示", "test-v1", "hash"), 0.9d));
        return new SkillRegistry(config, List.of(new KnowledgeSearchSkill(retriever, 2)));
    }

    private AgentContext context(AppConfig config) {
        return new AgentContextFactory(config).create(new AgentRequest(
                "请结合受控知识说明呼吸困难提示", "swarm-session", "swarm-user", List.of(), null, null, false, java.util.Map.of()
        ));
    }

    private LocalModelGateway gateway(AppConfig config, AtomicInteger calls, String response) {
        return new LocalModelGateway() {
            @Override
            public String provider() {
                return "swarm-test";
            }

            @Override
            public com.medicalagent.config.ModelConfig descriptor() {
                return config.getModel();
            }

            @Override
            public LocalModelResponse generate(LocalModelRequest request) {
                calls.incrementAndGet();
                return new LocalModelResponse(response, request.modelName(), provider(), 0L, request.prompt().length(), response.length());
            }
        };
    }

    private String retrieverSafetyPlan() {
        return "{\"mode\":\"swarm\",\"tasks\":[{\"role\":\"retriever\",\"query\":\"呼吸困难\"},{\"role\":\"safety\",\"query\":\"\"}]}";
    }
}
