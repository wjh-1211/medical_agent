package com.medicalagent.tracing;

import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelLoadListener;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;

import java.util.Map;

public record TracingLocalModelGateway(
        LocalModelGateway delegate,
        TraceSink traceSink,
        int maxPayloadCharacters,
        long slowCallMillis
) implements LocalModelGateway {

    @Override
    public String provider() {
        return delegate.provider();
    }

    @Override
    public com.medicalagent.config.ModelConfig descriptor() {
        return delegate.descriptor();
    }

    @Override
    public void preload() {
        delegate.preload();
    }

    @Override
    public void preload(LocalModelLoadListener listener) {
        delegate.preload(listener);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public LocalModelResponse generate(LocalModelRequest request) {
        long startedAt = System.nanoTime();
        String traceId = TraceScope.currentTraceId().orElse("unscoped");
        try {
            LocalModelResponse response = delegate.generate(request);
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            traceSink.record(TraceSupport.event(
                    traceId,
                    TraceEventType.MODEL_CALL,
                    "local_model.generate",
                    TraceStatus.SUCCEEDED,
                    elapsedMillis,
                    null,
                    request.prompt(),
                    response.content(),
                    Map.of(
                            "provider", response.provider(),
                            "model", response.modelName(),
                            "promptChars", Integer.toString(response.promptCharacters()),
                            "outputChars", Integer.toString(response.outputCharacters()),
                            "slow", Boolean.toString(elapsedMillis >= slowCallMillis)
                    ),
                    maxPayloadCharacters
            ));
            return response;
        } catch (RuntimeException exception) {
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            traceSink.record(TraceSupport.event(
                    traceId,
                    TraceEventType.MODEL_CALL,
                    "local_model.generate",
                    TraceStatus.FAILED,
                    elapsedMillis,
                    exception,
                    request.prompt(),
                    "",
                    Map.of("provider", provider(), "model", request.modelName()),
                    maxPayloadCharacters
            ));
            throw exception;
        }
    }
}
