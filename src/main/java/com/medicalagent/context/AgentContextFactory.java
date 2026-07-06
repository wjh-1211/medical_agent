package com.medicalagent.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.api.AgentMessage;
import com.medicalagent.api.AgentRequest;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.SessionConfig;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class AgentContextFactory {

    private final AppConfig config;
    private final Clock clock;
    private final Supplier<String> requestIdSupplier;
    private final Supplier<String> sessionIdSupplier;

    public AgentContextFactory(AppConfig config) {
        this(
                config,
                Clock.systemUTC(),
                () -> UUID.randomUUID().toString(),
                () -> UUID.randomUUID().toString()
        );
    }

    AgentContextFactory(
            AppConfig config,
            Clock clock,
            Supplier<String> requestIdSupplier,
            Supplier<String> sessionIdSupplier
    ) {
        this.config = config;
        this.clock = clock;
        this.requestIdSupplier = requestIdSupplier;
        this.sessionIdSupplier = sessionIdSupplier;
    }

    public AgentContext create(AgentRequest request) {
        String requestId = requestIdSupplier.get();
        String sessionId = resolveSessionId(request.sessionId());
        return AgentContext.builder()
                .requestId(requestId)
                .sessionId(sessionId)
                .userId(resolveUserId(request.userId(), sessionId))
                .message(requireText(request.message(), "request.message"))
                .history(normalizeHistory(request.history()))
                .memorySummary(normalizeOptionalText(request.memorySummary()))
                .toolFacts(normalizeToolFacts(request.toolFacts()))
                .emergencyFlag(request.emergencyFlag())
                .metadata(normalizeMetadata(request.metadata()))
                .createdAt(clock.instant())
                .requestTimeoutMillis(config.getApi().getRequestTimeoutMillis())
                .build();
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId.trim();
        }
        if (config.getSession().isAutoCreateSessionId()) {
            return sessionIdSupplier.get();
        }
        throw new IllegalArgumentException("request.sessionId must not be blank");
    }

    private String resolveUserId(String userId, String sessionId) {
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        SessionConfig sessionConfig = config.getSession();
        if (!sessionConfig.isAllowAnonymousUser()) {
            throw new IllegalArgumentException("request.userId must not be blank");
        }
        return sessionConfig.getAnonymousUserIdPrefix() + "-" + sessionId;
    }

    private List<AgentMessage> normalizeHistory(List<AgentMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        return history.stream()
                .map(this::normalizeMessage)
                .toList();
    }

    private AgentMessage normalizeMessage(AgentMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("request.history must not contain null items");
        }
        return new AgentMessage(
                requireText(message.role(), "request.history.role"),
                requireText(message.content(), "request.history.content")
        );
    }

    private JsonNode normalizeToolFacts(JsonNode toolFacts) {
        if (toolFacts == null) {
            return JsonSupport.NODE_FACTORY.objectNode();
        }
        if (!toolFacts.isObject()) {
            throw new IllegalArgumentException("request.toolFacts must be a JSON object");
        }
        return toolFacts.deepCopy();
    }

    private Map<String, String> normalizeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = requireText(entry.getKey(), "request.metadata key");
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            normalized.put(key, value);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
