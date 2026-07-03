package com.medicalagent.config;

public class RuntimeConfig {

    private String environment = "local";
    private int maxReActLoops = 5;
    private boolean toolLoggingEnabled = true;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public int getMaxReActLoops() {
        return maxReActLoops;
    }

    public void setMaxReActLoops(int maxReActLoops) {
        this.maxReActLoops = maxReActLoops;
    }

    public boolean isToolLoggingEnabled() {
        return toolLoggingEnabled;
    }

    public void setToolLoggingEnabled(boolean toolLoggingEnabled) {
        this.toolLoggingEnabled = toolLoggingEnabled;
    }
}
