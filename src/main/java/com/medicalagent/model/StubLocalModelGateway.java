package com.medicalagent.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
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
        String content = request.functionCallingEnabled()
                ? generateStructuredResponse(request.prompt())
                : "stub-response: " + extractCurrentMessage(request.prompt());
        return new LocalModelResponse(content, request.modelName(), provider());
    }

    private String generateStructuredResponse(String prompt) {
        if (hasObservation(prompt)) {
            return buildFinalAnswerResponse(extractLatestObservation(prompt));
        }
        return buildToolCallResponse(extractCurrentMessage(prompt));
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

    private boolean hasObservation(String prompt) {
        String observations = extractSection(prompt, "Observation History:", "Response Contract:");
        return observations != null && !observations.isBlank() && !observations.contains("No observations yet.");
    }

    private String extractLatestObservation(String prompt) {
        String observations = extractSection(prompt, "Observation History:", "Response Contract:");
        if (observations == null || observations.isBlank()) {
            return "No observations available";
        }
        String normalized = observations.trim().replaceAll("\\s+", " ");
        return normalized.substring(0, Math.min(200, normalized.length()));
    }

    private String extractSection(String prompt, String startMarker, String endMarker) {
        int startIndex = prompt.indexOf(startMarker);
        if (startIndex < 0) {
            return null;
        }
        String remaining = prompt.substring(startIndex + startMarker.length()).trim();
        int endIndex = remaining.indexOf(endMarker);
        return endIndex >= 0 ? remaining.substring(0, endIndex).trim() : remaining;
    }

    private String buildToolCallResponse(String message) {
        ObjectNode root = JsonSupport.NODE_FACTORY.objectNode();
        root.put("type", "tool_call");
        root.put("toolName", "uppercase_text");
        root.putObject("input").put("message", message);
        return root.toString();
    }

    private String buildFinalAnswerResponse(String observation) {
        ObjectNode root = JsonSupport.NODE_FACTORY.objectNode();
        root.put("type", "final_answer");
        root.put("answer", "stub-final-answer: " + observation);
        return root.toString();
    }
}
