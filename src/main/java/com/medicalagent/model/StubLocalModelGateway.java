package com.medicalagent.model;

import com.medicalagent.config.ModelConfig;

public class StubLocalModelGateway implements LocalModelGateway {

    private final ModelConfig modelConfig;

    public StubLocalModelGateway(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }

    @Override
    public String provider() {
        return "stub";
    }

    @Override
    public ModelConfig descriptor() {
        return modelConfig;
    }

    @Override
    public LocalModelResponse generate(LocalModelRequest request) {
        String content = "stub-response: " + extractCurrentMessage(request.prompt());
        return new LocalModelResponse(content, request.modelName(), provider());
    }

    private String extractCurrentMessage(String prompt) {
        String marker = "Current User Message:";
        int markerIndex = prompt.indexOf(marker);
        if (markerIndex < 0) {
            String compactPrompt = prompt.replaceAll("\\s+", " ").trim();
            return compactPrompt.substring(0, Math.min(160, compactPrompt.length()));
        }
        String remaining = prompt.substring(markerIndex + marker.length()).trim();
        int nextSection = remaining.indexOf("Conversation History:");
        String message = nextSection >= 0 ? remaining.substring(0, nextSection).trim() : remaining;
        return message.replaceAll("\\s+", " ");
    }
}
