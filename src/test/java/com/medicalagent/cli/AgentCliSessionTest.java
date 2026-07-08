package com.medicalagent.cli;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentCliSessionTest {

    @Test
    void shouldTrackHistoryAndResetSession() {
        AtomicInteger counter = new AtomicInteger(1);
        AgentCliSession session = new AgentCliSession("cli-user", () -> "session-" + counter.getAndIncrement());

        assertEquals("session-1", session.sessionId());

        session.appendTurn("hello", "hi");
        assertEquals(2, session.history().size());

        session.reset();

        assertEquals("session-2", session.sessionId());
        assertEquals(0, session.history().size());
    }
}
