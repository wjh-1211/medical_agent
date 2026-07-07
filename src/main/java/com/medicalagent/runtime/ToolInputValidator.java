package com.medicalagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.skills.ToolSchema;

import java.util.Iterator;
import java.util.Map;

public class ToolInputValidator {

    public void validate(ToolSchema toolSchema, JsonNode input) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException("Tool input must be a JSON object for tool: " + toolSchema.name());
        }

        JsonNode required = toolSchema.inputSchema().path("required");
        if (required.isArray()) {
            for (JsonNode field : required) {
                String fieldName = field.asText();
                if (!input.has(fieldName) || input.get(fieldName).isNull()) {
                    throw new IllegalArgumentException("Missing required field '" + fieldName + "' for tool: " + toolSchema.name());
                }
            }
        }

        JsonNode properties = toolSchema.inputSchema().path("properties");
        if (properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> property = fields.next();
                if (!input.has(property.getKey()) || input.get(property.getKey()).isNull()) {
                    continue;
                }
                validateType(toolSchema.name(), property.getKey(), property.getValue().path("type").asText(), input.get(property.getKey()));
            }
        }
    }

    private void validateType(String toolName, String fieldName, String expectedType, JsonNode actualValue) {
        boolean valid = switch (expectedType) {
            case "string" -> actualValue.isTextual();
            case "integer" -> actualValue.isIntegralNumber();
            case "number" -> actualValue.isNumber();
            case "boolean" -> actualValue.isBoolean();
            case "object" -> actualValue.isObject();
            case "array" -> actualValue.isArray();
            case "", "any" -> true;
            default -> true;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid field type for '" + fieldName + "' on tool " + toolName + ": expected " + expectedType
            );
        }
    }
}
