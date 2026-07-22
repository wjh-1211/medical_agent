package com.medicalagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.SkillRegistration;
import com.medicalagent.skills.ToolSchema;
import com.medicalagent.tracing.TraceEventType;
import com.medicalagent.tracing.TraceScope;
import com.medicalagent.tracing.TraceSink;
import com.medicalagent.tracing.TraceStatus;
import com.medicalagent.tracing.TraceSupport;

import java.util.Map;

public class ToolRouter {

    private final SkillRegistry skillRegistry;
    private final ToolInputValidator toolInputValidator;
    private final TraceSink traceSink;
    private final int maxPayloadCharacters;
    private final long slowCallMillis;

    public ToolRouter(SkillRegistry skillRegistry) {
        this(skillRegistry, new ToolInputValidator(), event -> { }, 320, 1000L);
    }

    public ToolRouter(SkillRegistry skillRegistry, ToolInputValidator toolInputValidator) {
        this(skillRegistry, toolInputValidator, event -> { }, 320, 1000L);
    }

    public ToolRouter(SkillRegistry skillRegistry, TraceSink traceSink, int maxPayloadCharacters) {
        this(skillRegistry, new ToolInputValidator(), traceSink, maxPayloadCharacters, 1000L);
    }

    public ToolRouter(SkillRegistry skillRegistry, TraceSink traceSink, int maxPayloadCharacters, long slowCallMillis) {
        this(skillRegistry, new ToolInputValidator(), traceSink, maxPayloadCharacters, slowCallMillis);
    }

    public ToolRouter(
            SkillRegistry skillRegistry,
            ToolInputValidator toolInputValidator,
            TraceSink traceSink,
            int maxPayloadCharacters,
            long slowCallMillis
    ) {
        this.skillRegistry = skillRegistry;
        this.toolInputValidator = toolInputValidator;
        this.traceSink = traceSink;
        this.maxPayloadCharacters = maxPayloadCharacters;
        this.slowCallMillis = slowCallMillis;
    }

    public JsonNode route(String toolName, JsonNode input) {
        long startedAt = System.nanoTime();
        String traceId = TraceScope.currentTraceId().orElse("unscoped");
        try {
            SkillRegistration registration = skillRegistry.findRegistrationByToolName(toolName)
                    .orElseThrow(() -> new IllegalArgumentException("No skill registered for tool: " + toolName));
            ToolSchema schema = skillRegistry.findSchemaByToolName(toolName)
                    .orElseThrow(() -> new IllegalStateException("No tool schema registered for tool: " + toolName));
            toolInputValidator.validate(schema, input);
            JsonNode result = registration.skill().execute(input);
            record(traceId, toolName, TraceStatus.SUCCEEDED, startedAt, null, input, result);
            return result;
        } catch (RuntimeException exception) {
            record(traceId, toolName, TraceStatus.FAILED, startedAt, exception, input, null);
            throw exception;
        }
    }

    private void record(
            String traceId,
            String toolName,
            TraceStatus status,
            long startedAt,
            RuntimeException error,
            JsonNode input,
            JsonNode result
    ) {
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        traceSink.record(TraceSupport.event(
                traceId,
                TraceEventType.TOOL_CALL,
                toolName,
                status,
                elapsedMillis,
                error,
                TraceSupport.summarizeJson(input, maxPayloadCharacters),
                result == null ? "" : TraceSupport.summarizeJson(result, maxPayloadCharacters),
                Map.of("slow", Boolean.toString(elapsedMillis >= slowCallMillis)),
                maxPayloadCharacters
        ));
    }
}
