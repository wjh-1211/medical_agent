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
        assertTrue(template.content().contains("Long-term Memory Policy"));
        assertTrue(template.content().contains("long_term_memory_write"));
        assertTrue(template.content().contains("长期记忆最终优先级"));
        assertTrue(template.content().contains("长期记忆召回最终优先级"));
        assertTrue(template.content().contains("Summary 查询最终优先级"));
        assertTrue(template.content().contains("Knowledge Retrieval Policy"));
        assertTrue(template.content().contains("knowledge_search"));
        assertTrue(template.content().contains("Never invent a guideline"));
        assertTrue(template.content().contains("最高优先级检索规则"));
        assertTrue(template.path().toString().endsWith("prompts/medical-agent-react.prompt.md"));
    }

    @Test
    void shouldLoadLongTermMemoryUpdatePrompt() {
        AppConfig config = new ConfigLoader().load("test");
        PromptTemplate template = new FilePromptLoader(config.getPrompt()).load("long-term-memory-update");

        assertTrue(template.content().contains("{{agentAnswer}}"));
        assertTrue(template.content().contains("long_term_memory_write"));
        assertTrue(template.content().contains("user_confirmed"));
    }

    @Test
    void shouldLoadSummaryMemoryUpdatePrompt() {
        AppConfig config = new ConfigLoader().load("test");
        PromptTemplate template = new FilePromptLoader(config.getPrompt()).load("summary-memory-update");

        assertTrue(template.content().contains("{{summaryMemory}}"));
        assertTrue(template.content().contains("summary_memory_write"));
        assertTrue(template.content().contains("future-useful session context"));
        assertTrue(template.content().contains("full transcript"));
        assertTrue(template.content().contains("repeated wording"));
        assertTrue(template.content().contains("最终优先级"));
    }

    @Test
    void shouldLoadSummaryContextAnswerPrompt() {
        AppConfig config = new ConfigLoader().load("test");
        PromptTemplate template = new FilePromptLoader(config.getPrompt()).load("summary-context-answer");

        assertTrue(template.content().contains("{{summaryMemory}}"));
        assertTrue(template.content().contains("summary_context_continue"));
        assertTrue(template.content().contains("最高体温38.5度"));
    }

    @Test
    void shouldLoadKnowledgeRetrievalDecisionPrompt() {
        AppConfig config = new ConfigLoader().load("test");
        PromptTemplate template = new FilePromptLoader(config.getPrompt()).load("knowledge-retrieval-decision");

        assertTrue(template.content().contains("{{message}}"));
        assertTrue(template.content().contains("knowledge_search"));
        assertTrue(template.content().contains("knowledge_retrieval_continue"));
        assertTrue(template.content().contains("受控知识库中，出现呼吸困难时有什么提示"));
    }
}
