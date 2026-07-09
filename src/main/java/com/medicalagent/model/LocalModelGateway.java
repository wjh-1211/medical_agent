package com.medicalagent.model;

import com.medicalagent.config.ModelConfig;

public interface LocalModelGateway {

    String provider();

    ModelConfig descriptor();

    default void preload() {
        // Default no-op for gateways that do not require explicit warmup.
    }

    default void preload(LocalModelLoadListener listener) {
        preload();
        if (listener != null) {
            listener.onProgress(100, "模型已就绪");
        }
    }

    default void shutdown() {
        // Default no-op for gateways that do not own external resources.
    }

    LocalModelResponse generate(LocalModelRequest request);
}
