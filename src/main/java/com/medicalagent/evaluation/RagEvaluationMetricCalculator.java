package com.medicalagent.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RagEvaluationMetricCalculator {

    private static final Pattern SOURCE_MARKER = Pattern.compile("\\[source: (.+?) \\| chunk: ([0-9a-f]+)]");

    public RagEvaluationMetrics calculate(List<RagEvaluationCaseResult> results, int topK) {
        List<RagEvaluationCaseResult> evidenceCases = results.stream()
                .filter(result -> !result.expectedChunkIds().isEmpty())
                .toList();
        List<RagEvaluationCaseResult> noResultCases = results.stream()
                .filter(result -> result.shouldRetrieve() && result.expectedChunkIds().isEmpty())
                .toList();

        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("retrieval.recall_at_1", rate(evidenceCases, result -> containsExpectedAtRank(result, 1)));
        metrics.put("retrieval.recall_at_" + topK, rate(evidenceCases, result -> containsExpectedAtRank(result, topK)));
        metrics.put("retrieval.mrr", mean(evidenceCases, this::reciprocalRank));
        metrics.put("retrieval.ndcg_at_" + topK, mean(evidenceCases, result -> normalizedDcg(result, topK)));
        metrics.put("retrieval.no_result_precision", rate(noResultCases, result -> result.actualChunkIds().isEmpty()));

        long truePositive = results.stream().filter(result -> result.shouldRetrieve() && result.toolCalled()).count();
        long falsePositive = results.stream().filter(result -> !result.shouldRetrieve() && result.toolCalled()).count();
        long falseNegative = results.stream().filter(result -> result.shouldRetrieve() && !result.toolCalled()).count();
        double precision = divide(truePositive, truePositive + falsePositive);
        double recall = divide(truePositive, truePositive + falseNegative);
        metrics.put("tool_selection.precision", precision);
        metrics.put("tool_selection.recall", recall);
        metrics.put("tool_selection.f1", precision + recall == 0d ? 0d : 2d * precision * recall / (precision + recall));

        List<RagEvaluationCaseResult> sourceRequired = results.stream()
                .filter(result -> !result.expectedChunkIds().isEmpty())
                .toList();
        metrics.put("answer.rubric_coverage", rate(sourceRequired, RagEvaluationCaseResult::rubricMatched));
        metrics.put("citation.validity", rate(sourceRequired, RagEvaluationCaseResult::citationValid));
        metrics.put("citation.coverage", rate(sourceRequired, RagEvaluationCaseResult::citationPresent));
        metrics.put("citation.unsupported_rate", rate(results, result -> result.citationPresent() && !result.citationValid()));
        metrics.put("answer.no_evidence_hallucination_rate", rate(noResultCases, result -> result.citationPresent()));

        metrics.put("latency.embedding_p50_ms", percentile(results, RagEvaluationCaseResult::embeddingMillis, 0.50d));
        metrics.put("latency.retrieval_p50_ms", percentile(results, RagEvaluationCaseResult::retrievalMillis, 0.50d));
        metrics.put("latency.total_p50_ms", percentile(results, RagEvaluationCaseResult::totalMillis, 0.50d));
        metrics.put("latency.total_p95_ms", percentile(results, RagEvaluationCaseResult::totalMillis, 0.95d));
        return new RagEvaluationMetrics(metrics);
    }

    public RagEvaluationCaseResult evaluateCase(RagEvaluationCase evaluationCase, RagEvaluationExecution execution) {
        List<String> actualChunkIds = execution.matches().stream().map(match -> match.chunk().chunkId()).toList();
        List<String> actualSources = execution.matches().stream().map(match -> match.chunk().source()).distinct().toList();
        Citation citation = extractCitation(execution.answer());
        boolean citationPresent = citation != null;
        boolean citationValid = citation != null
                && actualChunkIds.contains(citation.chunkId())
                && actualSources.contains(citation.source());
        boolean rubricMatched = evaluationCase.answerRubric().stream()
                .allMatch(term -> execution.answer().contains(term));
        List<String> failures = failureReasons(evaluationCase, execution, actualChunkIds, citationPresent, citationValid, rubricMatched);
        return new RagEvaluationCaseResult(
                evaluationCase.caseId(),
                evaluationCase.query(),
                evaluationCase.shouldRetrieve(),
                execution.toolCalled(),
                evaluationCase.expectedChunkIds(),
                actualChunkIds,
                actualSources,
                execution.answer(),
                citationPresent,
                citationValid,
                rubricMatched,
                execution.embeddingMillis(),
                execution.retrievalMillis(),
                execution.toolDecisionMillis(),
                execution.modelMillis(),
                execution.totalMillis(),
                failures
        );
    }

    private List<String> failureReasons(
            RagEvaluationCase evaluationCase,
            RagEvaluationExecution execution,
            List<String> actualChunkIds,
            boolean citationPresent,
            boolean citationValid,
            boolean rubricMatched
    ) {
        List<String> failures = new ArrayList<>();
        if (evaluationCase.shouldRetrieve() != execution.toolCalled()) {
            failures.add(evaluationCase.shouldRetrieve() ? "missing_tool_call" : "unexpected_tool_call");
        }
        if (!evaluationCase.expectedChunkIds().isEmpty() && actualChunkIds.stream().noneMatch(evaluationCase.expectedChunkIds()::contains)) {
            failures.add("expected_chunk_not_retrieved");
        }
        if (evaluationCase.expectedChunkIds().isEmpty() && execution.toolCalled() && !actualChunkIds.isEmpty()) {
            failures.add("unexpected_evidence");
        }
        if (!evaluationCase.expectedChunkIds().isEmpty() && !citationPresent) {
            failures.add("missing_citation");
        }
        if (citationPresent && !citationValid) {
            failures.add("unsupported_citation");
        }
        if (!evaluationCase.answerRubric().isEmpty() && !rubricMatched) {
            failures.add("answer_rubric_not_matched");
        }
        return failures;
    }

    private boolean containsExpectedAtRank(RagEvaluationCaseResult result, int rank) {
        return result.actualChunkIds().stream()
                .limit(rank)
                .anyMatch(result.expectedChunkIds()::contains);
    }

    private double reciprocalRank(RagEvaluationCaseResult result) {
        for (int index = 0; index < result.actualChunkIds().size(); index++) {
            if (result.expectedChunkIds().contains(result.actualChunkIds().get(index))) {
                return 1d / (index + 1);
            }
        }
        return 0d;
    }

    private double normalizedDcg(RagEvaluationCaseResult result, int topK) {
        double dcg = 0d;
        for (int index = 0; index < Math.min(topK, result.actualChunkIds().size()); index++) {
            if (result.expectedChunkIds().contains(result.actualChunkIds().get(index))) {
                dcg += 1d / log2(index + 2);
            }
        }
        int idealRelevant = Math.min(topK, result.expectedChunkIds().size());
        double idealDcg = 0d;
        for (int index = 0; index < idealRelevant; index++) {
            idealDcg += 1d / log2(index + 2);
        }
        return idealDcg == 0d ? 0d : dcg / idealDcg;
    }

    private double rate(Collection<RagEvaluationCaseResult> values, java.util.function.Predicate<RagEvaluationCaseResult> predicate) {
        return values.isEmpty() ? 0d : values.stream().filter(predicate).count() / (double) values.size();
    }

    private double mean(Collection<RagEvaluationCaseResult> values, java.util.function.ToDoubleFunction<RagEvaluationCaseResult> mapper) {
        return values.isEmpty() ? 0d : values.stream().mapToDouble(mapper).average().orElse(0d);
    }

    private double percentile(List<RagEvaluationCaseResult> values, java.util.function.ToLongFunction<RagEvaluationCaseResult> mapper, double percentile) {
        if (values.isEmpty()) {
            return 0d;
        }
        List<Long> sorted = values.stream().map(mapper::applyAsLong).sorted().toList();
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    private double divide(long numerator, long denominator) {
        return denominator == 0L ? 0d : numerator / (double) denominator;
    }

    private double log2(int value) {
        return Math.log(value) / Math.log(2d);
    }

    private Citation extractCitation(String answer) {
        Matcher matcher = SOURCE_MARKER.matcher(answer);
        return matcher.find() ? new Citation(matcher.group(1), matcher.group(2)) : null;
    }

    private record Citation(String source, String chunkId) {
    }
}
