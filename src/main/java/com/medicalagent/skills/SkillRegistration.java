package com.medicalagent.skills;

import com.medicalagent.config.SkillConfig;

public record SkillRegistration(
        Skill skill,
        ToolSchema toolSchema,
        SkillConfig skillConfig
) {
}
