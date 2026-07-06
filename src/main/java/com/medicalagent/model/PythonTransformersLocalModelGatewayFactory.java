package com.medicalagent.model;

import com.medicalagent.config.AppConfig;

public class PythonTransformersLocalModelGatewayFactory implements LocalModelGatewayFactory {

    @Override
    public String provider() {
        return "python_transformers";
    }

    @Override
    public LocalModelGateway create(AppConfig config) {
        return new PythonTransformersLocalModelGateway(config.getModel());
    }
}
