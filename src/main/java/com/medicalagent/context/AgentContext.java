package com.medicalagent.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.api.AgentMessage;
import com.medicalagent.common.JsonSupport;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AgentContext {

    private final String requestId;
    private final String sessionId;
    private final String userId;
    private final String message;
    private final List<AgentMessage> history;
    private final String memorySummary;
    private final JsonNode toolFacts;
    private final Boolean emergencyFlag;
    private final Map<String, String> metadata;
    private final Instant createdAt;
    private final int requestTimeoutMillis;

    private AgentContext(Builder builder) {
        this.requestId = Objects.requireNonNull(builder.requestId, "requestId must not be null");
        this.sessionId = Objects.requireNonNull(builder.sessionId, "sessionId must not be null");
        this.userId = Objects.requireNonNull(builder.userId, "userId must not be null");
        this.message = Objects.requireNonNull(builder.message, "message must not be null");
        this.history = List.copyOf(builder.history);
        this.memorySummary = builder.memorySummary;
        this.toolFacts = deepCopy(builder.toolFacts);
        this.emergencyFlag = builder.emergencyFlag;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt must not be null");
        this.requestTimeoutMillis = builder.requestTimeoutMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public List<AgentMessage> getHistory() {
        return history;
    }

    public String getMemorySummary() {
        return memorySummary;
    }

    public JsonNode getToolFacts() {
        return deepCopy(toolFacts);
    }

    public Boolean getEmergencyFlag() {
        return emergencyFlag;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    private static JsonNode deepCopy(JsonNode source) {
        if (source == null) {
            return JsonSupport.NODE_FACTORY.objectNode();
        }
        return source.deepCopy();
    }

    public static final class Builder {

        private String requestId;
        private String sessionId;
        private String userId;
        private String message;
        private List<AgentMessage> history = List.of();
        private String memorySummary;
        private JsonNode toolFacts = JsonSupport.NODE_FACTORY.objectNode();
        private Boolean emergencyFlag;
        private Map<String, String> metadata = Map.of();
        private Instant createdAt;
        private int requestTimeoutMillis;

        private Builder() {
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder history(List<AgentMessage> history) {
            this.history = history == null ? List.of() : history;
            return this;
        }

        public Builder memorySummary(String memorySummary) {
            this.memorySummary = memorySummary;
            return this;
        }

        public Builder toolFacts(JsonNode toolFacts) {
            this.toolFacts = toolFacts == null ? JsonSupport.NODE_FACTORY.objectNode() : toolFacts;
            return this;
        }

        public Builder emergencyFlag(Boolean emergencyFlag) {
            this.emergencyFlag = emergencyFlag;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata == null ? Map.of() : metadata;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder requestTimeoutMillis(int requestTimeoutMillis) {
            this.requestTimeoutMillis = requestTimeoutMillis;
            return this;
        }

        public AgentContext build() {
            return new AgentContext(this);
        }
    }
}
