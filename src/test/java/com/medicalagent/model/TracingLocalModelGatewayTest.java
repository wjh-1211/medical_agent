package com.medicalagent.model;

import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.tracing.InMemoryTraceSink;
import com.medicalagent.tracing.TraceErrorCategory;
import com.medicalagent.tracing.TraceScope;
import com.medicalagent.tracing.TraceStatus;
import com.medicalagent.tracing.TracingLocalModelGateway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TracingLocalModelGatewayTest {

    @Test
    void shouldTraceModelSuccessAndTimeoutFailure() {
        AppConfig config = new ConfigLoader().load("test");
        InMemoryTraceSink traceSink = new InMemoryTraceSink();
        LocalModelRequest request = new LocalModelRequest("prompt", "model", "path", 0.0, 16, true, 100);

        try (TraceScope ignored = TraceScope.open("trace-model-success")) {
            new TracingLocalModelGateway(new FixedGateway(config, false), traceSink, 32, 1)
                    .generate(request);
        }
        try (TraceScope ignored = TraceScope.open("trace-model-timeout")) {
            assertThrows(LocalModelException.class, () -> new TracingLocalModelGateway(
                    new FixedGateway(config, true), traceSink, 32, 1
            ).generate(request));
        }

        var success = traceSink.findByTraceId("trace-model-success").get(0);
        var failure = traceSink.findByTraceId("trace-model-timeout").get(0);
        assertEquals(TraceStatus.SUCCEEDED, success.status());
        assertEquals(TraceStatus.FAILED, failure.status());
        assertEquals(TraceErrorCategory.MODEL_TIMEOUT, failure.errorCategory());
    }

    private static final class FixedGateway implements LocalModelGateway {

        private final AppConfig config;
        private final boolean timeout;

        private FixedGateway(AppConfig config, boolean timeout) {
            this.config = config;
            this.timeout = timeout;
        }

        @Override
        public String provider() {
            return "trace-test";
        }

        @Override
        public com.medicalagent.config.ModelConfig descriptor() {
            return config.getModel();
        }

        @Override
        public LocalModelResponse generate(LocalModelRequest request) {
            if (timeout) {
                throw new LocalModelException("Local model timed out");
            }
            return new LocalModelResponse("{}", request.modelName(), provider(), 2L, request.prompt().length(), 2);
        }
    }
}
