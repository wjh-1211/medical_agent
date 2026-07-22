package com.medicalagent.swarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;

import java.util.List;

public record SwarmRoleResult(
        SwarmRole role,
        JsonNode result,
    List<JsonNode> observations
) {
    public SwarmRoleResult {
        result = result == null ? JsonSupport.NODE_FACTORY.objectNode() : result.deepCopy();
        observations = List.copyOf(observations == null
                ? List.of()
                : observations.stream().map(SwarmRoleResult::copyNode).toList());
    }

    private static JsonNode copyNode(JsonNode node) {
        return node == null ? JsonSupport.NODE_FACTORY.nullNode() : node.deepCopy();
    }
}
