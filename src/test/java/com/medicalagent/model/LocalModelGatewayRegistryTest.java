package com.medicalagent.model;

import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalModelGatewayRegistryTest {

    @Test
    void shouldCreateConfiguredProviderGateway() {
        AppConfig config = new ConfigLoader().load("test");
        LocalModelGateway gateway = new LocalModelGatewayRegistry(List.of(
                new StubLocalModelGatewayFactory(),
                new PythonTransformersLocalModelGatewayFactory()
        )).create(config);

        assertEquals("stub", gateway.provider());
        assertEquals("qwen3-test", gateway.descriptor().getName());
    }
}
