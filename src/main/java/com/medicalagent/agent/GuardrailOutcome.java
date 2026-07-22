package com.medicalagent.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record GuardrailOutcome(
        String answer,
        boolean emergency,
        List<JsonNode> results
) {
    public GuardrailOutcome {
        results = List.copyOf(results);
    }
}
