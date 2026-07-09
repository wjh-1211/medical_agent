package com.medicalagent.memory;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySessionMemoryStoreTest {

    @Test
    void shouldWriteAndReadSessionMemory() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-09T00:00:00Z"));
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(clock, Duration.ofMinutes(30));

        store.write("session-1", "I am allergic to penicillin", "I will remember that.");
        Optional<SessionMemorySnapshot> snapshot = store.read("session-1");

        assertTrue(snapshot.isPresent());
        assertTrue(snapshot.get().memorySummary().contains("penicillin"));
        assertEquals(Instant.parse("2026-07-09T00:30:00Z"), snapshot.get().expiresAt());
    }

    @Test
    void shouldExpireSessionMemoryAfterTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-09T00:00:00Z"));
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(clock, Duration.ofMinutes(1));

        store.write("session-1", "note", "answer");
        clock.advance(Duration.ofMinutes(2));

        assertTrue(store.read("session-1").isEmpty());
    }

    private static final class MutableClock extends Clock {

        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}
