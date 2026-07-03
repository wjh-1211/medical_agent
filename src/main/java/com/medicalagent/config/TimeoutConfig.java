package com.medicalagent.config;

public class TimeoutConfig {

    private int modelCallMillis = 15_000;
    private int toolCallMillis = 3_000;
    private int memoryCallMillis = 2_000;

    public int getModelCallMillis() {
        return modelCallMillis;
    }

    public void setModelCallMillis(int modelCallMillis) {
        this.modelCallMillis = modelCallMillis;
    }

    public int getToolCallMillis() {
        return toolCallMillis;
    }

    public void setToolCallMillis(int toolCallMillis) {
        this.toolCallMillis = toolCallMillis;
    }

    public int getMemoryCallMillis() {
        return memoryCallMillis;
    }

    public void setMemoryCallMillis(int memoryCallMillis) {
        this.memoryCallMillis = memoryCallMillis;
    }
}
