package com.medicalagent.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagEvaluationMetricCalculatorTest {

    @Test
    void shouldCalculateRetrievalToolCitationAndLatencyMetrics() {
        List<RagEvaluationCaseResult> results = List.of(
                result("hit", true, true, List.of("a"), List.of("a"), true, true, true, 10L),
                result("miss", true, false, List.of("b"), List.of(), false, false, false, 20L),
                result("no-result", true, true, List.of(), List.of(), false, false, true, 30L),
                result("no-tool", false, false, List.of(), List.of(), false, false, true, 40L)
        );

        var metrics = new RagEvaluationMetricCalculator().calculate(results, 3).values();

        assertEquals(0.5d, metrics.get("retrieval.recall_at_1"));
        assertEquals(0.5d, metrics.get("retrieval.mrr"));
        assertEquals(1d, metrics.get("retrieval.no_result_precision"));
        assertEquals(1d, metrics.get("tool_selection.precision"));
        assertEquals(2d / 3d, metrics.get("tool_selection.recall"));
        assertEquals(0.8d, metrics.get("tool_selection.f1"));
        assertEquals(0.5d, metrics.get("citation.validity"));
        assertEquals(20d, metrics.get("latency.total_p50_ms"));
        assertEquals(40d, metrics.get("latency.total_p95_ms"));
    }

    private RagEvaluationCaseResult result(
            String id,
            boolean shouldRetrieve,
            boolean toolCalled,
            List<String> expected,
            List<String> actual,
            boolean citationPresent,
            boolean citationValid,
            boolean rubricMatched,
            long totalMillis
    ) {
        return new RagEvaluationCaseResult(
                id,
                id,
                shouldRetrieve,
                toolCalled,
                expected,
                actual,
                citationValid ? List.of("source") : List.of(),
                "",
                citationPresent,
                citationValid,
                rubricMatched,
                0L,
                0L,
                0L,
                0L,
                totalMillis,
                List.of()
        );
    }
}
