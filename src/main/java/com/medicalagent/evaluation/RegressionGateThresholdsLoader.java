package com.medicalagent.evaluation;

import com.medicalagent.common.JsonSupport;

import java.io.IOException;
import java.nio.file.Path;

public class RegressionGateThresholdsLoader {

    public RegressionGateThresholds load(Path path) {
        try {
            RegressionGateThresholds thresholds = JsonSupport.YAML_MAPPER.readValue(path.toFile(), RegressionGateThresholds.class);
            if (thresholds.minimumMetrics().isEmpty()) {
                throw new IllegalArgumentException("Regression gate thresholds must define minimumMetrics");
            }
            return thresholds;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read regression gate thresholds: " + path, exception);
        }
    }
}
