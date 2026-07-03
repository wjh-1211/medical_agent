package com.medicalagent.config;

public class ModelConfig {

    private String provider = "local";
    private String name = "qwen3";
    private double temperature = 0.2;
    private int maxTokens = 2048;
    private boolean functionCallingEnabled = true;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isFunctionCallingEnabled() {
        return functionCallingEnabled;
    }

    public void setFunctionCallingEnabled(boolean functionCallingEnabled) {
        this.functionCallingEnabled = functionCallingEnabled;
    }
}
