package com.medicalagent.prompt;

import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePromptLoaderTest {

    @Test
    void shouldLoadPromptTemplateFromConfiguredDirectory() {
        AppConfig config = new ConfigLoader().load("test");
        PromptTemplate template = new FilePromptLoader(config.getPrompt()).load(config.getPrompt().getDefaultTemplate());

        assertEquals("medical-agent-answer", template.name());
        assertTrue(template.content().contains("{{message}}"));
        assertTrue(template.path().toString().endsWith("prompts/medical-agent-answer.prompt.md"));
    }
}
