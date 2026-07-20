package com.medicalagent.evaluation;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RagEvaluationRunner {

    private final RagEvaluationMetricCalculator metricCalculator;

    public RagEvaluationRunner(RagEvaluationMetricCalculator metricCalculator) {
        this.metricCalculator = metricCalculator;
    }

    public RagEvaluationReport run(
            RagEvaluationCaseSet caseSet,
            RagEvaluationCandidate candidate,
            int topK,
            String evaluatorVersion,
            Map<String, String> runtimeMetadata,
            Optional<RagEvaluationReport> baseline
    ) {
        List<RagEvaluationCaseResult> results = caseSet.cases().stream()
                .map(evaluationCase -> metricCalculator.evaluateCase(evaluationCase, candidate.execute(evaluationCase)))
                .toList();
        RagEvaluationMetrics metrics = metricCalculator.calculate(results, topK);
        Map<String, Double> baselineMetrics = baseline.map(report -> report.metrics().values()).orElse(Map.of());
        Map<String, Double> deltas = calculateDeltas(metrics.values(), baselineMetrics);
        String comparisonStatus = baseline.isPresent()
                ? "candidate_compared_to_baseline"
                : candidate.deterministic() ? "baseline_created" : "local_sample_no_comparison";
        return new RagEvaluationReport(
                "T013",
                evaluatorVersion,
                caseSet.caseSetVersion(),
                caseSet.corpusVersion(),
                candidate.candidateId(),
                candidate.mode(),
                candidate.deterministic(),
                Instant.now().toString(),
                results.size(),
                metrics,
                comparisonStatus,
                baselineMetrics,
                deltas,
                runtimeMetadata,
                results
        );
    }

    private Map<String, Double> calculateDeltas(Map<String, Double> metrics, Map<String, Double> baselineMetrics) {
        Map<String, Double> deltas = new LinkedHashMap<>();
        for (Map.Entry<String, Double> metric : metrics.entrySet()) {
            if (baselineMetrics.containsKey(metric.getKey())) {
                deltas.put(metric.getKey(), metric.getValue() - baselineMetrics.get(metric.getKey()));
            }
        }
        return deltas;
    }
}
