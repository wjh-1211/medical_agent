package com.medicalagent.skills;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ToolSchemaRegistry {

    private final Map<String, ToolSchema> schemasByName = new LinkedHashMap<>();

    public ToolSchemaRegistry(Collection<SkillRegistration> registrations) {
        for (SkillRegistration registration : registrations) {
            ToolSchema schema = registration.toolSchema();
            validate(schema);
            ToolSchema existing = schemasByName.putIfAbsent(schema.name(), schema);
            if (existing != null) {
                throw new IllegalArgumentException("Duplicate tool schema registered for tool: " + schema.name());
            }
        }
    }

    public Optional<ToolSchema> findByName(String toolName) {
        return Optional.ofNullable(schemasByName.get(toolName));
    }

    public Map<String, ToolSchema> registeredSchemas() {
        return Map.copyOf(schemasByName);
    }

    private void validate(ToolSchema schema) {
        if (schema.name() == null || schema.name().isBlank()) {
            throw new IllegalArgumentException("Tool schema name must not be blank");
        }
        if (schema.description() == null || schema.description().isBlank()) {
            throw new IllegalArgumentException("Tool schema description must not be blank for tool: " + schema.name());
        }
        if (!schema.inputSchema().isObject()) {
            throw new IllegalArgumentException("Tool schema inputSchema must be a JSON object for tool: " + schema.name());
        }
        if (!"object".equals(schema.inputSchema().path("type").asText())) {
            throw new IllegalArgumentException("Tool schema type must be 'object' for tool: " + schema.name());
        }
    }
}
