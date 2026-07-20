package com.medicalagent.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.KnowledgeConfig;
import com.medicalagent.model.LocalModelException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PythonTransformersEmbeddingModel implements QueryEmbeddingModel {

    private final KnowledgeConfig config;
    private final Object lifecycleLock = new Object();
    private Process process;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private volatile String lastServerError = "";

    public PythonTransformersEmbeddingModel(KnowledgeConfig config) {
        this.config = config;
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        List<String> texts = segments.stream().map(TextSegment::text).toList();
        return Response.from(embed(texts, "document"));
    }

    @Override
    public Response<Embedding> embedQuery(String query) {
        return Response.from(embed(List.of(query), "query").get(0));
    }

    @Override
    public int dimension() {
        return config.getEmbeddingDimension();
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            shutdownServer();
        }
    }

    private List<Embedding> embed(List<String> texts, String mode) {
        synchronized (lifecycleLock) {
            ensureStarted();
            String requestId = UUID.randomUUID().toString();
            try {
                ObjectNode payload = JsonSupport.NODE_FACTORY.objectNode();
                payload.put("type", "embed");
                payload.put("requestId", requestId);
                payload.put("mode", mode);
                ArrayNode serializedTexts = payload.putArray("texts");
                texts.forEach(serializedTexts::add);
                processInput.write(JsonSupport.JSON_MAPPER.writeValueAsString(payload));
                processInput.newLine();
                processInput.flush();
                JsonNode response = waitForResponse(requestId);
                List<Embedding> embeddings = new ArrayList<>();
                for (JsonNode vector : response.path("vectors")) {
                    float[] values = new float[vector.size()];
                    for (int index = 0; index < vector.size(); index++) {
                        values[index] = (float) vector.get(index).asDouble();
                    }
                    embeddings.add(Embedding.from(values));
                }
                if (embeddings.size() != texts.size()) {
                    throw new LocalModelException("Embedding model returned an unexpected vector count");
                }
                return embeddings;
            } catch (IOException exception) {
                shutdownServer();
                throw new LocalModelException("Failed to communicate with local embedding process", exception);
            }
        }
    }

    private void ensureStarted() {
        if (process != null && process.isAlive() && processInput != null && processOutput != null) {
            return;
        }
        ProcessBuilder processBuilder = new ProcessBuilder(
                config.getEmbeddingPythonExecutable(),
                config.getEmbeddingLauncherScript(),
                "--server"
        );
        processBuilder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
        try {
            process = processBuilder.start();
            processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            processOutput = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            startErrorDrain(process.getErrorStream());
            ObjectNode init = JsonSupport.NODE_FACTORY.objectNode();
            init.put("type", "init");
            init.put("modelPath", config.getEmbeddingModelPath());
            init.put("dimension", config.getEmbeddingDimension());
            init.put("queryInstruction", config.getQueryInstruction());
            processInput.write(JsonSupport.JSON_MAPPER.writeValueAsString(init));
            processInput.newLine();
            processInput.flush();
            waitForReady();
        } catch (IOException exception) {
            shutdownServer();
            throw new LocalModelException("Failed to start local embedding process", exception);
        }
    }

    private void waitForReady() throws IOException {
        Instant deadline = Instant.now().plusSeconds(120);
        while (Instant.now().isBefore(deadline)) {
            ensureServerAvailable();
            if (!processOutput.ready()) {
                sleepQuietly(25L);
                continue;
            }
            JsonNode message = JsonSupport.JSON_MAPPER.readTree(processOutput.readLine());
            if ("ready".equals(message.path("type").asText())) {
                return;
            }
            if ("error".equals(message.path("type").asText())) {
                throw new LocalModelException(message.path("message").asText("Embedding startup failed"));
            }
        }
        throw new LocalModelException("Local embedding model startup timed out");
    }

    private JsonNode waitForResponse(String requestId) throws IOException {
        Instant deadline = Instant.now().plusSeconds(60);
        while (Instant.now().isBefore(deadline)) {
            ensureServerAvailable();
            if (!processOutput.ready()) {
                sleepQuietly(25L);
                continue;
            }
            JsonNode message = JsonSupport.JSON_MAPPER.readTree(processOutput.readLine());
            if ("response".equals(message.path("type").asText()) && requestId.equals(message.path("requestId").asText())) {
                return message;
            }
            if ("error".equals(message.path("type").asText())) {
                throw new LocalModelException(message.path("message").asText("Embedding request failed"));
            }
        }
        throw new LocalModelException("Local embedding model call timed out");
    }

    private void ensureServerAvailable() {
        if (process == null || !process.isAlive()) {
            throw new LocalModelException("Local embedding process is not available: " + lastServerError);
        }
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
        }, "local-embedding-stderr");
        worker.setDaemon(true);
        worker.start();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LocalModelException("Local embedding process was interrupted", exception);
        }
    }
}
