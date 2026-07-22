package com.medicalagent.evaluation;

import java.util.List;
import java.util.Map;

public record RegressionGateReport(
        String taskId,
        String evaluatorVersion,
        String generatedAt,
        boolean passed,
        Map<String, String> reportInputs,
        Map<String, Double> metrics,
        List<RegressionGateCheck> checks,
        List<String> failureReasons
) {
    public RegressionGateReport {
        reportInputs = reportInputs == null ? Map.of() : Map.copyOf(reportInputs);
        metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
        checks = checks == null ? List.of() : List.copyOf(checks);
        failureReasons = failureReasons == null ? List.of() : List.copyOf(failureReasons);
    }
}
