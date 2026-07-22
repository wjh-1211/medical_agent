package com.medicalagent.config;

public class SwarmConfig {

    private boolean enabled = false;
    private int maxRoles = 4;
    private int maxPlanSteps = 4;
    private int roleTimeoutMillis = 3000;
    private boolean fallbackToSingleAgent = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRoles() {
        return maxRoles;
    }

    public void setMaxRoles(int maxRoles) {
        this.maxRoles = maxRoles;
    }

    public int getMaxPlanSteps() {
        return maxPlanSteps;
    }

    public void setMaxPlanSteps(int maxPlanSteps) {
        this.maxPlanSteps = maxPlanSteps;
    }

    public int getRoleTimeoutMillis() {
        return roleTimeoutMillis;
    }

    public void setRoleTimeoutMillis(int roleTimeoutMillis) {
        this.roleTimeoutMillis = roleTimeoutMillis;
    }

    public boolean isFallbackToSingleAgent() {
        return fallbackToSingleAgent;
    }

    public void setFallbackToSingleAgent(boolean fallbackToSingleAgent) {
        this.fallbackToSingleAgent = fallbackToSingleAgent;
    }
}
