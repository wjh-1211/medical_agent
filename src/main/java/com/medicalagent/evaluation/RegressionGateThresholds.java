package com.medicalagent.evaluation;

import java.util.Map;

public record RegressionGateThresholds(String version, Map<String, Double> minimumMetrics) {

    public RegressionGateThresholds {
        version = version == null || version.isBlank() ? "unknown" : version;
        minimumMetrics = minimumMetrics == null ? Map.of() : Map.copyOf(minimumMetrics);
    }
}
