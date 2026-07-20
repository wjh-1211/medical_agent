package com.medicalagent.evaluation;

import java.util.List;
import java.util.Map;

public record RagEvaluationReport(
        String taskId,
        String evaluatorVersion,
        String caseSetVersion,
        String corpusVersion,
        String candidateId,
        String mode,
        boolean deterministic,
        String generatedAt,
        int caseCount,
        RagEvaluationMetrics metrics,
        String comparisonStatus,
        Map<String, Double> baselineMetrics,
        Map<String, Double> metricDeltas,
        Map<String, String> runtimeMetadata,
        List<RagEvaluationCaseResult> cases
) {
    public RagEvaluationReport {
        baselineMetrics = baselineMetrics == null ? Map.of() : Map.copyOf(baselineMetrics);
        metricDeltas = metricDeltas == null ? Map.of() : Map.copyOf(metricDeltas);
        runtimeMetadata = runtimeMetadata == null ? Map.of() : Map.copyOf(runtimeMetadata);
        cases = cases == null ? List.of() : List.copyOf(cases);
    }
}
