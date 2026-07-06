package com.medicalagent.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public record AgentRequest(
        String message,
        String sessionId,
        String userId,
        List<AgentMessage> history,
        String memorySummary,
        JsonNode toolFacts,
        Boolean emergencyFlag,
        Map<String, String> metadata
) {
}
