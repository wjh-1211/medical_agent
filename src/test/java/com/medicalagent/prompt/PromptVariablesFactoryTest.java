package com.medicalagent.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.api.AgentMessage;
import com.medicalagent.api.AgentRequest;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.config.ContextConfig;
import com.medicalagent.context.AgentContext;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.skills.ToolSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptVariablesFactoryTest {

    @Test
    void shouldKeepOnlyRecentHistoryAndObservations() {
        AppConfig config = new ConfigLoader().load("test");
        List<AgentMessage> history = List.of(
                new AgentMessage("user", "m1"),
                new AgentMessage("assistant", "m2"),
                new AgentMessage("user", "m3"),
                new AgentMessage("assistant", "m4"),
                new AgentMessage("user", "m5")
        );
        AgentContext context = new AgentContextFactory(config).create(new AgentRequest(
                "latest",
                "session-9",
                "user-9",
                history,
                "summary",
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "cli")
        ));
        List<JsonNode> observations = List.of(
                JsonSupport.NODE_FACTORY.objectNode().put("index", 1),
                JsonSupport.NODE_FACTORY.objectNode().put("index", 2),
                JsonSupport.NODE_FACTORY.objectNode().put("index", 3)
        );

        Map<String, String> variables = new PromptVariablesFactory().create(context, List.of(), observations);

        assertFalse(variables.get("history").contains("m1"));
        assertTrue(variables.get("history").contains("m2"));
        assertFalse(variables.get("observations").contains("\"index\":1"));
        assertTrue(variables.get("observations").contains("\"index\":2"));
        assertTrue(variables.get("observations").contains("\"index\":3"));
    }

    @Test
    void shouldRenderAvailableToolsInCompactForm() {
        AppConfig config = new ConfigLoader().load("test");
        AgentContext context = new AgentContextFactory(config).create(new AgentRequest(
                "hello",
                "session-9",
                "user-9",
                List.of(),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "cli")
        ));
        ToolSchema schema = new ToolSchema(
                "sample_tool",
                "A sample tool for testing compact formatting.",
                JsonSupport.NODE_FACTORY.objectNode()
                        .put("type", "object")
                        .set("properties", JsonSupport.NODE_FACTORY.objectNode()
                                .putObject("message").put("type", "string")
                                .putObject("level").put("type", "string"))
        );

        Map<String, String> variables = new PromptVariablesFactory().create(context, List.of(schema), List.of());

        assertTrue(variables.get("availableTools").contains("sample_tool("));
        assertFalse(variables.get("availableTools").contains("\"properties\""));
    }

    @Test
    void shouldPrioritizeMemorySectionsOverRawHistoryAndToolFacts() {
        AppConfig config = new ConfigLoader().load("test");
        ContextConfig budget = new ContextConfig();
        budget.setRecentHistoryMaxMessages(2);
        budget.setRecentHistoryMaxCharacters(80);
        budget.setLongTermMemoryMaxCharacters(120);
        budget.setSessionMemoryMaxCharacters(100);
        budget.setSummaryMemoryMaxCharacters(120);
        budget.setToolFactsMaxCharacters(60);
        ObjectNode toolFacts = JsonSupport.NODE_FACTORY.objectNode();
        ObjectNode longTermMemory = toolFacts.putObject("longTermMemory");
        longTermMemory.put("found", true);
        longTermMemory.putArray("records").addObject().put("fact", "Allergic to penicillin");
        ObjectNode summaryMemory = toolFacts.putObject("summaryMemory");
        summaryMemory.put("found", true);
        summaryMemory.put("summary", "Confirmed fever reached 38.5C");
        toolFacts.put("unrelated", "this generic tool fact is lower priority");
        AgentContext context = new AgentContextFactory(config).create(new AgentRequest(
                "latest question",
                "session-1",
                "user-1",
                List.of(
                        new AgentMessage("user", "original-history-that-should-not-survive-the-recent-history-budget"),
                        new AgentMessage("assistant", "older answer"),
                        new AgentMessage("user", "recent detail"),
                        new AgentMessage("assistant", "recent reply")
                ),
                null,
                toolFacts,
                false,
                Map.of("channel", "test")
        ));

        Map<String, String> variables = new PromptVariablesFactory(budget).create(context, List.of(), List.of());

        assertTrue(variables.get("longTermMemory").contains("Allergic to penicillin"));
        assertTrue(variables.get("summaryMemory").contains("Confirmed fever reached 38.5C"));
        assertFalse(variables.get("history").contains("original-history-that-should-not-survive"));
        assertFalse(variables.get("toolFacts").contains("longTermMemory"));
        assertFalse(variables.get("toolFacts").contains("summaryMemory"));
    }
}
