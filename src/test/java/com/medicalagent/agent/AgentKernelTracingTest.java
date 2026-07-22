package com.medicalagent.agent;

import com.medicalagent.api.AgentRequest;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.EmergencyDetectionSkill;
import com.medicalagent.skills.GroundingCheckSkill;
import com.medicalagent.skills.RiskAssessmentSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.tracing.InMemoryTraceSink;
import com.medicalagent.tracing.TraceErrorCategory;
import com.medicalagent.tracing.TraceEventType;
import com.medicalagent.tracing.TraceStatus;
import com.medicalagent.tracing.TracingLocalModelGateway;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentKernelTracingTest {

    @Test
    void shouldCorrelateRequestAndInvalidModelOutput() {
        AppConfig config = new ConfigLoader().load("test");
        InMemoryTraceSink traceSink = new InMemoryTraceSink();
        SkillRegistry registry = new SkillRegistry(config, List.of(new EchoSkill()));
        LocalModelGateway modelGateway = new TracingLocalModelGateway(
                new InvalidJsonGateway(config), traceSink, 64, 1
        );
        AgentKernel kernel = new AgentKernel(
                config,
                registry,
                new ToolRouter(registry, traceSink, 64, 1),
                new FilePromptLoader(config.getPrompt()),
                new PromptVariablesFactory(config.getContext()),
                new PromptRenderer(config.getPrompt()),
                modelGateway,
                new AgentDecisionParser(),
                traceSink
        );
        var context = new AgentContextFactory(config).create(new AgentRequest(
                "trace this request", "trace-session", "trace-user", List.of(), null, null, false, Map.of("channel", "test")
        ));

        assertThrows(IllegalArgumentException.class, () -> kernel.handle(context));

        String traceId = context.getMetadata().get("traceId");
        var events = traceSink.findByTraceId(traceId);
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.REQUEST_STARTED));
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.MODEL_CALL && event.status() == TraceStatus.SUCCEEDED));
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.REQUEST_FAILED
                && event.errorCategory() == TraceErrorCategory.MODEL_INVALID_OUTPUT));
        assertTrue(events.stream().allMatch(event -> traceId.equals(event.traceId())));
    }

    @Test
    void shouldRecordBlockedGuardrailAction() {
        AppConfig config = new ConfigLoader().load("test");
        InMemoryTraceSink traceSink = new InMemoryTraceSink();
        SkillRegistry registry = new SkillRegistry(config, List.of(
                new EmergencyDetectionSkill(), new RiskAssessmentSkill(), new GroundingCheckSkill()
        ));
        LocalModelGateway modelGateway = new TracingLocalModelGateway(
                new SequenceGateway(config, List.of(
                        "{\"type\":\"final_answer\",\"answer\":\"请在家观察。\"}",
                        "{\"emergency\":true,\"urgency\":\"emergency\",\"reason\":\"fixture emergency\"}"
                )), traceSink, 64, 1
        );
        AgentKernel kernel = new AgentKernel(
                config, registry, new ToolRouter(registry, traceSink, 64, 1),
                new FilePromptLoader(config.getPrompt()), new PromptVariablesFactory(config.getContext()),
                new PromptRenderer(config.getPrompt()), modelGateway, new AgentDecisionParser(), traceSink
        );
        var context = new AgentContextFactory(config).create(new AgentRequest(
                "胸痛伴呼吸困难", "trace-guardrail-session", "trace-guardrail-user", List.of(), null, null, false, Map.of("channel", "test")
        ));

        var response = kernel.handle(context);
        var events = traceSink.findByTraceId(response.traceId());

        assertEquals(config.getGuardrail().getEmergencySafeAnswer(), response.answer());
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.GUARDRAIL_ACTION
                && event.status() == TraceStatus.BLOCKED));
        assertTrue(events.stream().anyMatch(event -> event.type() == TraceEventType.TOOL_CALL
                && "emergency_detection".equals(event.name())));
    }

    private static final class InvalidJsonGateway implements LocalModelGateway {

        private final AppConfig config;

        private InvalidJsonGateway(AppConfig config) {
            this.config = config;
        }

        @Override
        public String provider() {
            return "invalid-json-test";
        }

        @Override
        public com.medicalagent.config.ModelConfig descriptor() {
            return config.getModel();
        }

        @Override
        public LocalModelResponse generate(LocalModelRequest request) {
            return new LocalModelResponse("not-json", request.modelName(), provider(), 1L, request.prompt().length(), 8);
        }
    }

    private static final class SequenceGateway implements LocalModelGateway {

        private final AppConfig config;
        private final Queue<String> responses;

        private SequenceGateway(AppConfig config, List<String> responses) {
            this.config = config;
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public String provider() {
            return "trace-sequence";
        }

        @Override
        public com.medicalagent.config.ModelConfig descriptor() {
            return config.getModel();
        }

        @Override
        public LocalModelResponse generate(LocalModelRequest request) {
            String content = responses.remove();
            return new LocalModelResponse(content, request.modelName(), provider(), 1L, request.prompt().length(), content.length());
        }
    }
}
