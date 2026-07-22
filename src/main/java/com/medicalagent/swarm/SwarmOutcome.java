package com.medicalagent.swarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;

import java.util.List;

public record SwarmOutcome(
        boolean activated,
        List<SwarmRoleResult> roleResults,
        List<JsonNode> observations,
        String fallbackReason
) {
    public SwarmOutcome {
        roleResults = List.copyOf(roleResults == null ? List.of() : roleResults);
        observations = List.copyOf(observations == null
                ? List.of()
                : observations.stream().map(SwarmOutcome::copyNode).toList());
        fallbackReason = fallbackReason == null ? "" : fallbackReason;
    }

    private static JsonNode copyNode(JsonNode node) {
        return node == null ? JsonSupport.NODE_FACTORY.nullNode() : node.deepCopy();
    }

    public static SwarmOutcome singleAgent(String reason) {
        return new SwarmOutcome(false, List.of(), List.of(), reason);
    }

    public boolean hasKnowledgeObservation() {
        return observations.stream().anyMatch(observation -> "knowledge_search".equals(observation.path("toolName").asText()));
    }
}
