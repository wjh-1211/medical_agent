package com.medicalagent.evaluation;

import com.medicalagent.common.JsonSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class RegressionGateReportWriter {

    public void writeJson(Path path, RegressionGateReport report) {
        try {
            createParent(path);
            JsonSupport.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), report);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write regression gate report: " + path, exception);
        }
    }

    public void writeMarkdown(Path path, RegressionGateReport report) {
        StringBuilder summary = new StringBuilder("# T017 Regression Gate Summary\n\n");
        summary.append("- Status: ").append(report.passed() ? "PASSED" : "FAILED").append("\n");
        summary.append("- Evaluator: ").append(report.evaluatorVersion()).append("\n\n");
        summary.append("## Checks\n\n");
        report.checks().forEach(check -> summary.append("- ").append(check.metric())
                .append(": actual=").append(String.format("%.4f", check.actual()))
                .append(", minimum=").append(String.format("%.4f", check.minimum()))
                .append(", result=").append(check.passed() ? "passed" : "failed")
                .append(check.failureReason().isBlank() ? "" : ", reason=" + check.failureReason())
                .append("\n"));
        if (!report.failureReasons().isEmpty()) {
            summary.append("\n## Failures\n\n");
            report.failureReasons().forEach(reason -> summary.append("- ").append(reason).append("\n"));
        }
        try {
            createParent(path);
            Files.writeString(path, summary.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write regression gate summary: " + path, exception);
        }
    }

    private void createParent(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
