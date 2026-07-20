package com.medicalagent.evaluation;

import java.util.Map;

public record RagEvaluationMetrics(Map<String, Double> values) {
    public RagEvaluationMetrics {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
