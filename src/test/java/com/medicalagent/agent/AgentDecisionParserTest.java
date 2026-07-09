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
    void shouldParseJsonWrappedInAdditionalText() {
        AgentDecision decision = parser.parse("""
                下面是最终结果：
                ```json
                {
                  "type": "final_answer",
                  "answer": "Wrapped answer"
                }
                ```
                """);

        assertEquals(AgentDecision.Type.FINAL_ANSWER, decision.type());
        assertEquals("Wrapped answer", decision.answer());
    }

    @Test
    void shouldPreferLastValidDecisionObject() {
        AgentDecision decision = parser.parse("""
                先给一个错误示例：
                {"type":"tool_call","toolName":"echo_input","input":{"message":"example"}}

                实际输出如下：
                {"type":"final_answer","answer":"最终答案"}
                """);

        assertEquals(AgentDecision.Type.FINAL_ANSWER, decision.type());
        assertEquals("最终答案", decision.answer());
    }

    @Test
    void shouldRejectInvalidDecisionPayload() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(JsonSupport.JSON_MAPPER.createArrayNode().toString())
        );

        assertEquals("Model response must be a JSON object", exception.getMessage());
    }

    @Test
    void shouldExposeSnippetWhenNoJsonCanBeRecovered() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("this is not json at all")
        );

        assertEquals("Model response must be valid JSON: this is not json at all", exception.getMessage());
    }
}
