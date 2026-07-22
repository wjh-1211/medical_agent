package com.medicalagent.evaluation;

public record RegressionGateCheck(
        String metric,
        double actual,
        double minimum,
        boolean passed,
        String failureReason
) {
    public RegressionGateCheck {
        failureReason = failureReason == null ? "" : failureReason;
    }
}
