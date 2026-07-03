package com.medicalagent.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class AppConfig {

    private RuntimeConfig runtime = new RuntimeConfig();
    private ModelConfig model = new ModelConfig();
    private MemoryConfig memory = new MemoryConfig();
    private GuardrailConfig guardrail = new GuardrailConfig();
    private TimeoutConfig timeout = new TimeoutConfig();
    private CacheConfig cache = new CacheConfig();
    private Map<String, SkillConfig> skills = new LinkedHashMap<>();

    public RuntimeConfig getRuntime() {
        return runtime;
    }

    public void setRuntime(RuntimeConfig runtime) {
        this.runtime = runtime;
    }

    public ModelConfig getModel() {
        return model;
    }

    public void setModel(ModelConfig model) {
        this.model = model;
    }

    public MemoryConfig getMemory() {
        return memory;
    }

    public void setMemory(MemoryConfig memory) {
        this.memory = memory;
    }

    public GuardrailConfig getGuardrail() {
        return guardrail;
    }

    public void setGuardrail(GuardrailConfig guardrail) {
        this.guardrail = guardrail;
    }

    public TimeoutConfig getTimeout() {
        return timeout;
    }

    public void setTimeout(TimeoutConfig timeout) {
        this.timeout = timeout;
    }

    public CacheConfig getCache() {
        return cache;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    public Map<String, SkillConfig> getSkills() {
        return skills;
    }

    public void setSkills(Map<String, SkillConfig> skills) {
        this.skills = skills;
    }
}
