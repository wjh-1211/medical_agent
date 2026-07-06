package com.medicalagent.model;

import com.medicalagent.config.AppConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LocalModelGatewayRegistry {

    private final Map<String, LocalModelGatewayFactory> factories = new LinkedHashMap<>();

    public LocalModelGatewayRegistry(List<LocalModelGatewayFactory> factories) {
        for (LocalModelGatewayFactory factory : factories) {
            this.factories.put(factory.provider(), factory);
        }
    }

    public LocalModelGateway create(AppConfig config) {
        String provider = config.getModel().getProvider();
        LocalModelGatewayFactory factory = factories.get(provider);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported local model provider: " + provider);
        }
        return factory.create(config);
    }
}
