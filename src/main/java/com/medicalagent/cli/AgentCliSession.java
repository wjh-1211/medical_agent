package com.medicalagent.cli;

import com.medicalagent.api.AgentMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class AgentCliSession {

    private final String userId;
    private final Supplier<String> sessionIdSupplier;
    private String sessionId;
    private final List<AgentMessage> history = new ArrayList<>();

    public AgentCliSession(String userId) {
        this(userId, () -> UUID.randomUUID().toString());
    }

    AgentCliSession(String userId, Supplier<String> sessionIdSupplier) {
        this.userId = userId == null || userId.isBlank() ? "cli-user" : userId.trim();
        this.sessionIdSupplier = sessionIdSupplier;
        this.sessionId = sessionIdSupplier.get();
    }

    public String sessionId() {
        return sessionId;
    }

    public String userId() {
        return userId;
    }

    public List<AgentMessage> history() {
        return List.copyOf(history);
    }

    public void appendTurn(String userMessage, String agentAnswer) {
        history.add(new AgentMessage("user", userMessage));
        history.add(new AgentMessage("assistant", agentAnswer));
    }

    public void reset() {
        history.clear();
        sessionId = sessionIdSupplier.get();
    }
}
