package com.medicalagent.model;

import com.medicalagent.config.AppConfig;

public class StubLocalModelGatewayFactory implements LocalModelGatewayFactory {

    @Override
    public String provider() {
        return "stub";
    }

    @Override
    public LocalModelGateway create(AppConfig config) {
        return new StubLocalModelGateway(config.getModel());
    }
}
