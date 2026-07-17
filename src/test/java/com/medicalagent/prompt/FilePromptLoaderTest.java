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

        assertEquals("medical-agent-react", template.name());
        assertTrue(template.content().contains("{{message}}"));
        assertTrue(template.content().contains("{{availableTools}}"));
        assertTrue(template.content().contains("{{observations}}"));
        assertTrue(template.content().contains("minimum safety information"));
        assertTrue(template.content().contains("Do not repeat a previous generic self-care answer"));
        assertTrue(template.content().contains("最高优先级医疗交互规则"));
        assertTrue(template.content().contains("必须先追问"));
        assertTrue(template.content().contains("A symptom-only message such as"));
        assertTrue(template.content().contains("do not ask which medication they want"));
        assertTrue(template.path().toString().endsWith("prompts/medical-agent-react.prompt.md"));
    }
}
