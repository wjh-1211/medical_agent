package com.medicalagent.memory;

import java.util.Optional;

public interface SessionMemoryStore {

    Optional<SessionMemorySnapshot> read(String sessionId);

    SessionMemorySnapshot write(String sessionId, String userMessage, String agentAnswer);
}
