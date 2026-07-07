package com.medicalagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;

public record AgentDecision(
        Type type,
        String answer,
        String toolName,
        JsonNode input
) {

    public AgentDecision {
        input = input == null ? JsonSupport.NODE_FACTORY.objectNode() : input.deepCopy();
    }

    public static AgentDecision finalAnswer(String answer) {
        return new AgentDecision(Type.FINAL_ANSWER, answer, null, JsonSupport.NODE_FACTORY.objectNode());
    }

    public static AgentDecision toolCall(String toolName, JsonNode input) {
        return new AgentDecision(Type.TOOL_CALL, null, toolName, input);
    }

    public enum Type {
        TOOL_CALL("tool_call"),
        FINAL_ANSWER("final_answer");

        private final String wireValue;

        Type(String wireValue) {
            this.wireValue = wireValue;
        }

        public String wireValue() {
            return wireValue;
        }

        public static Type fromWireValue(String value) {
            for (Type type : values()) {
                if (type.wireValue.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unsupported agent decision type: " + value);
        }
    }
}
