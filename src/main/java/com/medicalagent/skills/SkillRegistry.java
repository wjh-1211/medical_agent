package com.medicalagent.skills;

import com.medicalagent.config.AppConfig;
import com.medicalagent.config.SkillConfig;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SkillRegistry {

    private final Map<String, SkillRegistration> registrationsByToolName = new LinkedHashMap<>();
    private final ToolSchemaRegistry toolSchemaRegistry;

    public SkillRegistry(AppConfig appConfig, Iterable<Skill> skills) {
        Map<String, SkillConfig> configuredSkills = appConfig.getSkills();
        Map<String, Skill> skillsById = new LinkedHashMap<>();
        for (Skill skill : skills) {
            Skill existingSkill = skillsById.putIfAbsent(skill.id(), skill);
            if (existingSkill != null) {
                throw new IllegalArgumentException("Duplicate skill id registered: " + skill.id());
            }
        }
        for (Map.Entry<String, SkillConfig> entry : configuredSkills.entrySet()) {
            Skill skill = skillsById.get(entry.getKey());
            if (skill == null) {
                continue;
            }
            SkillConfig skillConfig = entry.getValue();
            if (skillConfig == null || !skillConfig.isEnabled()) {
                continue;
            }
            String toolName = skillConfig.getToolName() == null || skillConfig.getToolName().isBlank()
                    ? skill.toolSchema().name()
                    : skillConfig.getToolName();
            ToolSchema resolvedSchema = skill.toolSchema().withName(toolName);
            SkillRegistration existingRegistration = registrationsByToolName.putIfAbsent(
                    toolName,
                    new SkillRegistration(skill, resolvedSchema, skillConfig)
            );
            if (existingRegistration != null) {
                throw new IllegalArgumentException("Duplicate tool registration for tool: " + toolName);
            }
        }
        this.toolSchemaRegistry = new ToolSchemaRegistry(registrationsByToolName.values());
    }

    public Optional<Skill> findByToolName(String toolName) {
        return findRegistrationByToolName(toolName).map(SkillRegistration::skill);
    }

    public Optional<SkillRegistration> findRegistrationByToolName(String toolName) {
        return Optional.ofNullable(registrationsByToolName.get(toolName));
    }

    public Optional<ToolSchema> findSchemaByToolName(String toolName) {
        return toolSchemaRegistry.findByName(toolName);
    }

    public Map<String, Skill> registeredTools() {
        Map<String, Skill> tools = new LinkedHashMap<>();
        for (Map.Entry<String, SkillRegistration> entry : registrationsByToolName.entrySet()) {
            tools.put(entry.getKey(), entry.getValue().skill());
        }
        return Map.copyOf(tools);
    }

    public Collection<SkillRegistration> registrations() {
        return registrationsByToolName.values();
    }

    public Collection<ToolSchema> registeredToolSchemas() {
        return toolSchemaRegistry.registeredSchemas().values();
    }

    public ToolSchemaRegistry toolSchemaRegistry() {
        return toolSchemaRegistry;
    }
}
