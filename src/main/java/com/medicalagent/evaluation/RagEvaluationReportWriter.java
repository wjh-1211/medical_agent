package com.medicalagent.evaluation;

import com.medicalagent.common.JsonSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

public class RagEvaluationReportWriter {

    public void writeJson(Path path, RagEvaluationReport report) {
        try {
            createParent(path);
            JsonSupport.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), report);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write RAG evaluation report: " + path, exception);
        }
    }

    public RagEvaluationReport readJson(Path path) {
        try {
            return JsonSupport.JSON_MAPPER.readValue(path.toFile(), RagEvaluationReport.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read RAG evaluation report: " + path, exception);
        }
    }

    public void writeMarkdownSummary(Path path, RagEvaluationReport report) {
        StringBuilder summary = new StringBuilder();
        summary.append("# T013 RAG Evaluation Summary\n\n");
        summary.append("- Candidate: ").append(report.candidateId()).append("\n");
        summary.append("- Mode: ").append(report.mode()).append("\n");
        summary.append("- Deterministic: ").append(report.deterministic()).append("\n");
        summary.append("- Case set: ").append(report.caseSetVersion()).append("\n");
        summary.append("- Corpus: ").append(report.corpusVersion()).append("\n");
        summary.append("- Comparison: ").append(report.comparisonStatus()).append("\n\n");
        summary.append("## Metrics\n\n");
        report.metrics().values().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(metric -> summary.append("- ").append(metric.getKey()).append(": ")
                        .append(String.format("%.4f", metric.getValue())).append("\n"));
        if (!report.metricDeltas().isEmpty()) {
            summary.append("\n## Deltas\n\n");
            report.metricDeltas().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(metric -> summary.append("- ").append(metric.getKey()).append(": ")
                            .append(String.format("%+.4f", metric.getValue())).append("\n"));
        }
        summary.append("\n## Failures\n\n");
        report.cases().stream()
                .filter(result -> !result.failureReasons().isEmpty())
                .sorted(Comparator.comparing(RagEvaluationCaseResult::caseId))
                .forEach(result -> summary.append("- ").append(result.caseId()).append(": ")
                        .append(String.join(", ", result.failureReasons())).append("\n"));
        try {
            createParent(path);
            Files.writeString(path, summary.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write RAG evaluation summary: " + path, exception);
        }
    }

    private void createParent(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
