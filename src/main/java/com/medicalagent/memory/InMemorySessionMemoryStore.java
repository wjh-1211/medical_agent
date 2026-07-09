package com.medicalagent.memory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionMemoryStore implements SessionMemoryStore {

    private static final int MAX_SUMMARY_LENGTH = 160;

    private final Clock clock;
    private final Duration ttl;
    private final Map<String, SessionMemorySnapshot> recordsBySessionId = new ConcurrentHashMap<>();

    public InMemorySessionMemoryStore(Duration ttl) {
        this(Clock.systemUTC(), ttl);
    }

    public InMemorySessionMemoryStore(Clock clock, Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public Optional<SessionMemorySnapshot> read(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        SessionMemorySnapshot snapshot = recordsBySessionId.get(sessionId);
        if (snapshot == null) {
            return Optional.empty();
        }
        if (snapshot.expiresAt().isBefore(clock.instant())) {
            recordsBySessionId.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    @Override
    public SessionMemorySnapshot write(String sessionId, String userMessage, String agentAnswer) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        Instant now = clock.instant();
        SessionMemorySnapshot snapshot = new SessionMemorySnapshot(
                sessionId,
                buildSummary(userMessage, agentAnswer),
                normalize(userMessage),
                normalize(agentAnswer),
                now,
                now.plus(ttl)
        );
        recordsBySessionId.put(sessionId, snapshot);
        return snapshot;
    }

    private String buildSummary(String userMessage, String agentAnswer) {
        return "Recent session notes:" + System.lineSeparator()
                + "- Last user concern: " + normalize(userMessage) + System.lineSeparator()
                + "- Last agent guidance: " + normalize(agentAnswer);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        if (compact.length() <= MAX_SUMMARY_LENGTH) {
            return compact;
        }
        return compact.substring(0, MAX_SUMMARY_LENGTH - 3) + "...";
    }
}
