package com.medicalagent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLoaderTest {

    @Test
    void shouldLoadAndMergeProfileConfig() {
        AppConfig config = new ConfigLoader().load("test");

        assertEquals("test", config.getRuntime().getEnvironment());
        assertEquals(3, config.getRuntime().getMaxReActLoops());
        assertEquals("qwen3-test", config.getModel().getName());
        assertEquals(1000, config.getTimeout().getToolCallMillis());
    }

    @Test
    void shouldRejectInvalidConfigValues() throws Exception {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("config/invalid-application.yml")) {
            JsonNode invalidNode = JsonSupport.YAML_MAPPER.readTree(inputStream);
            AppConfig invalidConfig = JsonSupport.YAML_MAPPER.convertValue(invalidNode, AppConfig.class);
            ConfigLoader loader = new ConfigLoader();

            assertThrows(ConfigException.class, () -> loader.validate(invalidConfig));
        }
    }
}
