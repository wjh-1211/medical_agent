package com.medicalagent.model;

import com.medicalagent.config.ModelConfig;

public interface LocalModelGateway {

    String provider();

    ModelConfig descriptor();

    LocalModelResponse generate(LocalModelRequest request);
}
