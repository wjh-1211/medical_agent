package com.medicalagent.model;

public record LocalModelRequest(
        String prompt,
        String modelName,
        String modelPath,
        double temperature,
        int maxTokens,
        boolean functionCallingEnabled,
        int timeoutMillis
) {
}
