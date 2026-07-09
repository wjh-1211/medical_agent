package com.medicalagent.model;

public record LocalModelResponse(
        String content,
        String modelName,
        String provider,
        long elapsedMillis,
        int promptCharacters,
        int outputCharacters
) {
}
