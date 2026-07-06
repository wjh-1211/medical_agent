package com.medicalagent.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.ModelConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class PythonTransformersLocalModelGateway implements LocalModelGateway {

    private final ModelConfig modelConfig;

    public PythonTransformersLocalModelGateway(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }

    @Override
    public String provider() {
        return "python_transformers";
    }

    @Override
    public ModelConfig descriptor() {
        return modelConfig;
    }

    @Override
    public LocalModelResponse generate(LocalModelRequest request) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                modelConfig.getPythonExecutable(),
                modelConfig.getLauncherScript()
        );
        processBuilder.directory(Path.of(".").toAbsolutePath().normalize().toFile());

        try {
            Process process = processBuilder.start();
            writeRequest(process, request);
            boolean finished = process.waitFor(request.timeoutMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new LocalModelException("Local model call timed out after " + request.timeoutMillis() + " ms");
            }
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new LocalModelException("Local model process failed: " + stderr);
            }
            JsonNode responseJson = JsonSupport.JSON_MAPPER.readTree(stdout);
            return new LocalModelResponse(
                    responseJson.path("content").asText(),
                    responseJson.path("modelName").asText(request.modelName()),
                    responseJson.path("provider").asText(provider())
            );
        } catch (IOException exception) {
            throw new LocalModelException("Failed to start local model process", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LocalModelException("Local model process was interrupted", exception);
        }
    }

    private void writeRequest(Process process, LocalModelRequest request) throws IOException {
        ObjectNode payload = JsonSupport.NODE_FACTORY.objectNode();
        payload.put("prompt", request.prompt());
        payload.put("modelName", request.modelName());
        payload.put("modelPath", request.modelPath());
        payload.put("temperature", request.temperature());
        payload.put("maxTokens", request.maxTokens());
        payload.put("functionCallingEnabled", request.functionCallingEnabled());
        try (OutputStream outputStream = process.getOutputStream()) {
            outputStream.write(JsonSupport.JSON_MAPPER.writeValueAsBytes(payload));
            outputStream.flush();
        }
    }
}
