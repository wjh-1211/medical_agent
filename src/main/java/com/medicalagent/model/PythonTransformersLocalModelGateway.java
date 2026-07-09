package com.medicalagent.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.ModelConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PythonTransformersLocalModelGateway implements LocalModelGateway {

    private final ModelConfig modelConfig;
    private final Object lifecycleLock = new Object();
    private Process process;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private volatile String lastServerError = "";

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
    public void preload() {
        preload(null);
    }

    @Override
    public void preload(LocalModelLoadListener listener) {
        synchronized (lifecycleLock) {
            if (isServerReady()) {
                if (listener != null) {
                    listener.onProgress(100, "模型已就绪");
                }
                return;
            }
            startServer(listener);
        }
    }

    @Override
    public LocalModelResponse generate(LocalModelRequest request) {
        preload();
        synchronized (lifecycleLock) {
            ensureServerAvailable();
            String requestId = UUID.randomUUID().toString();
            long startedAt = System.nanoTime();
            try {
                ObjectNode payload = JsonSupport.NODE_FACTORY.objectNode();
                payload.put("type", "generate");
                payload.put("requestId", requestId);
                payload.put("prompt", request.prompt());
                payload.put("modelName", request.modelName());
                payload.put("modelPath", request.modelPath());
                payload.put("temperature", request.temperature());
                payload.put("maxTokens", request.maxTokens());
                payload.put("functionCallingEnabled", request.functionCallingEnabled());
                processInput.write(JsonSupport.JSON_MAPPER.writeValueAsString(payload));
                processInput.newLine();
                processInput.flush();

                JsonNode responseJson = waitForResponse(requestId, request.timeoutMillis());
                String content = responseJson.path("content").asText();
                return new LocalModelResponse(
                        content,
                        responseJson.path("modelName").asText(request.modelName()),
                        responseJson.path("provider").asText(provider()),
                        (System.nanoTime() - startedAt) / 1_000_000L,
                        request.prompt() == null ? 0 : request.prompt().length(),
                        content.length()
                );
            } catch (IOException exception) {
                shutdownServer();
                throw new LocalModelException("Failed to communicate with local model process", exception);
            }
        }
    }

    @Override
    public void shutdown() {
        synchronized (lifecycleLock) {
            shutdownServer();
        }
    }

    private void startServer(LocalModelLoadListener listener) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                modelConfig.getPythonExecutable(),
                modelConfig.getLauncherScript(),
                "--server"
        );
        processBuilder.directory(Path.of(".").toAbsolutePath().normalize().toFile());

        try {
            process = processBuilder.start();
            processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            processOutput = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            startErrorDrain(process.getErrorStream());
            sendInitCommand();
            waitForReady(listener);
        } catch (IOException exception) {
            shutdownServer();
            throw new LocalModelException("Failed to start local model process", exception);
        }
    }

    private void waitForReady(LocalModelLoadListener listener) throws IOException {
        while (true) {
            ensureServerAvailable();
            if (!processOutput.ready()) {
                sleepQuietly(25L);
                continue;
            }
            String line = processOutput.readLine();
            if (line == null) {
                shutdownServer();
                throw new LocalModelException("Local model process exited during startup: " + lastServerError);
            }
            JsonNode message = JsonSupport.JSON_MAPPER.readTree(line);
            String type = message.path("type").asText();
            if ("status".equals(type) && listener != null) {
                listener.onProgress(
                        message.path("progress").asInt(0),
                        message.path("message").asText("模型加载中")
                );
                continue;
            }
            if ("ready".equals(type)) {
                if (listener != null) {
                    listener.onProgress(
                            message.path("progress").asInt(100),
                            message.path("message").asText("模型已就绪")
                    );
                }
                return;
            }
            if ("error".equals(type)) {
                shutdownServer();
                throw new LocalModelException(message.path("message").asText("Local model startup failed"));
            }
        }
    }

    private void sendInitCommand() throws IOException {
        ObjectNode payload = JsonSupport.NODE_FACTORY.objectNode();
        payload.put("type", "init");
        payload.put("modelName", modelConfig.getName());
        payload.put("modelPath", modelConfig.getPath());
        processInput.write(JsonSupport.JSON_MAPPER.writeValueAsString(payload));
        processInput.newLine();
        processInput.flush();
    }

    private JsonNode waitForResponse(String requestId, int timeoutMillis) throws IOException {
        Instant deadline = Instant.now().plusMillis(timeoutMillis);
        while (Instant.now().isBefore(deadline)) {
            ensureServerAvailable();
            if (!processOutput.ready()) {
                sleepQuietly(25L);
                continue;
            }
            String line = processOutput.readLine();
            if (line == null) {
                shutdownServer();
                throw new LocalModelException("Local model process exited while generating response: " + lastServerError);
            }
            JsonNode message = JsonSupport.JSON_MAPPER.readTree(line);
            String type = message.path("type").asText();
            if ("response".equals(type) && requestId.equals(message.path("requestId").asText())) {
                return message;
            }
            if ("error".equals(type)) {
                shutdownServer();
                throw new LocalModelException(message.path("message").asText("Local model process failed"));
            }
        }
        shutdownServer();
        throw new LocalModelException("Local model call timed out after " + timeoutMillis + " ms");
    }

    private void ensureServerAvailable() {
        if (process == null || !process.isAlive()) {
            throw new LocalModelException("Local model process is not available: " + lastServerError);
        }
    }

    private boolean isServerReady() {
        return process != null && process.isAlive() && processInput != null && processOutput != null;
    }

    private void shutdownServer() {
        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(1, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        process = null;
        processInput = null;
        processOutput = null;
    }

    private void startErrorDrain(InputStream errorStream) {
        Thread worker = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lastServerError = line;
                }
            } catch (IOException ignored) {
                // Best-effort stderr draining only.
            }
        }, "local-model-stderr");
        worker.setDaemon(true);
        worker.start();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LocalModelException("Local model process was interrupted", exception);
        }
    }
}
