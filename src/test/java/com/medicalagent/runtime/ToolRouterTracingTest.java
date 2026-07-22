package com.medicalagent.runtime;

import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.tracing.InMemoryTraceSink;
import com.medicalagent.tracing.TraceErrorCategory;
import com.medicalagent.tracing.TraceScope;
import com.medicalagent.tracing.TraceStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRouterTracingTest {

    @Test
    void shouldRecordSuccessfulAndFailedToolCallsWithActiveTraceId() {
        AppConfig config = new ConfigLoader().load("test");
        InMemoryTraceSink traceSink = new InMemoryTraceSink();
        ToolRouter router = new ToolRouter(
                new SkillRegistry(config, List.of(new EchoSkill())),
                traceSink,
                24,
                1L
        );
        var input = JsonSupport.NODE_FACTORY.objectNode().put("message", "a message that exceeds the summary limit");

        try (TraceScope ignored = TraceScope.open("trace-tool")) {
            router.route("echo_input", input);
            try {
                router.route("missing_tool", input);
            } catch (IllegalArgumentException expected) {
                // Assert through the trace below.
            }
        }

        var events = traceSink.findByTraceId("trace-tool");
        assertEquals(2, events.size());
        assertEquals(TraceStatus.SUCCEEDED, events.get(0).status());
        assertTrue(events.get(0).inputSummary().length() <= 24);
        assertEquals(TraceStatus.FAILED, events.get(1).status());
        assertEquals(TraceErrorCategory.TOOL_EXECUTION, events.get(1).errorCategory());
    }
}
