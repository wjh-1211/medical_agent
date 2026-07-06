package com.medicalagent.context;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.api.AgentMessage;
import com.medicalagent.api.AgentRequest;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentContextFactoryTest {

    @Test
    void shouldBuildIndependentContextsWithReservedFields() {
        AppConfig config = new ConfigLoader().load("test");
        Clock clock = Clock.fixed(Instant.parse("2026-07-05T08:00:00Z"), ZoneOffset.UTC);
        AtomicInteger requestSequence = new AtomicInteger();
        AtomicInteger sessionSequence = new AtomicInteger();
        AgentContextFactory factory = new AgentContextFactory(
                config,
                clock,
                () -> "req-" + requestSequence.incrementAndGet(),
                () -> "session-" + sessionSequence.incrementAndGet()
        );

        ObjectNode toolFacts = JsonSupport.NODE_FACTORY.objectNode();
        toolFacts.put("triage", "pending");
        AgentRequest request = new AgentRequest(
                "I have a cough",
                null,
                null,
                List.of(new AgentMessage("user", "Cough for two days")),
                "Recent respiratory symptoms",
                toolFacts,
                null,
                Map.of("channel", "web")
        );

        AgentContext first = factory.create(request);
        toolFacts.put("triage", "mutated");
        AgentContext second = factory.create(request);

        assertEquals("req-1", first.getRequestId());
        assertEquals("session-1", first.getSessionId());
        assertEquals("anonymous-session-1", first.getUserId());
        assertEquals("I have a cough", first.getMessage());
        assertEquals("Recent respiratory symptoms", first.getMemorySummary());
        assertEquals("pending", first.getToolFacts().get("triage").asText());
        assertNull(first.getEmergencyFlag());
        assertEquals("web", first.getMetadata().get("channel"));
        assertEquals(1, first.getHistory().size());
        assertEquals("2026-07-05T08:00:00Z", first.getCreatedAt().toString());
        assertEquals(config.getApi().getRequestTimeoutMillis(), first.getRequestTimeoutMillis());

        assertEquals("req-2", second.getRequestId());
        assertEquals("session-2", second.getSessionId());
        assertNotEquals(first.getRequestId(), second.getRequestId());
        assertNotEquals(first.getSessionId(), second.getSessionId());
        assertThrows(UnsupportedOperationException.class, () -> first.getHistory().add(new AgentMessage("assistant", "extra")));
    }

    @Test
    void shouldRejectNonObjectToolFacts() {
        AppConfig config = new ConfigLoader().load("test");
        AgentContextFactory factory = new AgentContextFactory(config);

        AgentRequest request = new AgentRequest(
                "hello",
                "session-1",
                "user-1",
                List.of(),
                null,
                JsonSupport.NODE_FACTORY.textNode("invalid"),
                null,
                Map.of()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> factory.create(request));

        assertEquals("request.toolFacts must be a JSON object", exception.getMessage());
    }
}
