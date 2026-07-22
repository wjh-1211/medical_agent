package com.medicalagent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

    @Test
    void shouldLoadAndMergeProfileConfig() {
        AppConfig config = new ConfigLoader().load("test");

        assertEquals("test", config.getRuntime().getEnvironment());
        assertEquals("/api/agent", config.getApi().getBasePath());
        assertEquals("anonymous", config.getSession().getAnonymousUserIdPrefix());
        assertEquals("prompts", config.getPrompt().getDirectory());
        assertEquals("medical-agent-react", config.getPrompt().getDefaultTemplate());
        assertEquals("stub", config.getModel().getProvider());
        assertEquals(3, config.getRuntime().getMaxReActLoops());
        assertEquals("qwen3-test", config.getModel().getName());
        assertEquals(1000, config.getTimeout().getToolCallMillis());
        assertEquals("sqlite", config.getMemory().getLongTermStore());
        assertEquals("target/test-long-term-memory.db", config.getMemory().getLongTermSqlitePath());
        assertEquals("sqlite", config.getMemory().getSummaryStore());
        assertEquals("target/test-summary-memory.db", config.getMemory().getSummarySqlitePath());
        assertEquals(4, config.getContext().getSummaryUpdateMinHistoryMessages());
        assertEquals("hash", config.getKnowledge().getEmbeddingProvider());
        assertEquals(64, config.getKnowledge().getEmbeddingDimension());
        assertEquals("src/test/resources/knowledge", config.getKnowledge().getDocumentsDirectory());
        assertTrue(config.getTracing().isEnabled());
        assertEquals(320, config.getTracing().getMaxPayloadCharacters());
        assertTrue(!config.getSwarm().isEnabled());
        assertEquals(4, config.getSwarm().getMaxRoles());
        assertTrue(config.getGuardrail().isBlockOnDecisionFailure());
        assertEquals("当前描述可能存在紧急风险。请尽快联系当地急救服务或立即前往急诊；如身边有人，请让对方协助，不要独自驾驶。", config.getGuardrail().getEmergencySafeAnswer());
    }

    @Test
    void shouldEnableDedicatedSwarmCliProfile() {
        AppConfig config = new ConfigLoader().load("swarm-test");

        assertTrue(config.getSwarm().isEnabled());
        assertEquals("stub", config.getModel().getProvider());
        assertEquals("swarm-test", config.getRuntime().getEnvironment());
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
