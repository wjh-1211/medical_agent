package com.medicalagent.evaluation;

import java.nio.file.Path;

/** CLI entry point used after the deterministic RAG evaluation has refreshed its report. */
public class RegressionGateApplication {

    public static void main(String[] args) {
        Path thresholdsPath = path("regression.thresholdsPath", "evaluation/regression-thresholds.yml");
        Path ragReportPath = path("regression.ragReportPath", "evaluation-report.json");
        Path guardrailReportPath = path("regression.guardrailReportPath", "evaluation/guardrail-evaluation-report.json");
        Path runtimeReportPath = path("regression.runtimeReportPath", "evaluation/runtime-observability-report.json");
        Path swarmReportPath = path("regression.swarmReportPath", "evaluation/swarm-observability-report.json");
        Path outputPath = path("regression.outputPath", "evaluation/evaluation-report.json");
        Path summaryPath = path("regression.summaryPath", "worklog/T017_REGRESSION_GATE_SUMMARY.md");

        RegressionGateThresholds thresholds = new RegressionGateThresholdsLoader().load(thresholdsPath);
        RegressionGateReport report = new RegressionGateRunner().run(
                thresholds, ragReportPath, guardrailReportPath, runtimeReportPath, swarmReportPath
        );
        RegressionGateReportWriter writer = new RegressionGateReportWriter();
        writer.writeJson(outputPath, report);
        writer.writeMarkdown(summaryPath, report);
        System.out.println("Regression gate " + (report.passed() ? "PASSED" : "FAILED") + ": " + outputPath);
        if (!report.passed()) {
            throw new IllegalStateException("Regression gate failed: " + String.join("; ", report.failureReasons()));
        }
    }

    private static Path path(String property, String fallback) {
        return Path.of(System.getProperty(property, fallback));
    }
}
