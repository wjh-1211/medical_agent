package com.medicalagent.agent;

import com.medicalagent.common.JsonSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentDecisionParserTest {

    private final AgentDecisionParser parser = new AgentDecisionParser();

    @Test
    void shouldParseToolCallDecision() {
        AgentDecision decision = parser.parse("""
                {
                  "type": "tool_call",
                  "toolName": "uppercase_text",
                  "input": {
                    "message": "fever"
                  }
                }
                """);

        assertEquals(AgentDecision.Type.TOOL_CALL, decision.type());
        assertEquals("uppercase_text", decision.toolName());
        assertEquals("fever", decision.input().path("message").asText());
    }

    @Test
    void shouldParseFinalAnswerDecision() {
        AgentDecision decision = parser.parse("""
                {
                  "type": "final_answer",
                  "answer": "Grounded answer"
                }
                """);

        assertEquals(AgentDecision.Type.FINAL_ANSWER, decision.type());
        assertEquals("Grounded answer", decision.answer());
    }

    @Test
    void shouldRejectInvalidDecisionPayload() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(JsonSupport.JSON_MAPPER.createArrayNode().toString())
        );

        assertEquals("Model response must be a JSON object", exception.getMessage());
    }
}
