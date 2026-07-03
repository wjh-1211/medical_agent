package com.medicalagent.skills;

import com.medicalagent.config.AppConfig;
import com.medicalagent.config.SkillConfig;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SkillRegistry {

    private final Map<String, Skill> toolsByName = new LinkedHashMap<>();

    public SkillRegistry(AppConfig appConfig, Iterable<Skill> skills) {
        Map<String, SkillConfig> configuredSkills = appConfig.getSkills();
        for (Skill skill : skills) {
            SkillConfig skillConfig = configuredSkills.get(skill.id());
            if (skillConfig == null || !skillConfig.isEnabled()) {
                continue;
            }
            String toolName = skillConfig.getToolName() == null || skillConfig.getToolName().isBlank()
                    ? skill.tool().name()
                    : skillConfig.getToolName();
            toolsByName.put(toolName, skill);
        }
    }

    public Optional<Skill> findByToolName(String toolName) {
        return Optional.ofNullable(toolsByName.get(toolName));
    }

    public Map<String, Skill> registeredTools() {
        return Map.copyOf(toolsByName);
    }
}
