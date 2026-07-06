package com.medicalagent.model;

import com.medicalagent.config.AppConfig;

public interface LocalModelGatewayFactory {

    String provider();

    LocalModelGateway create(AppConfig config);
}
