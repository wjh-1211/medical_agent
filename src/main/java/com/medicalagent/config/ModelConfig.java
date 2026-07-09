package com.medicalagent.config;

public class ModelConfig {

    private String provider = "local";
    private String name = "qwen3";
    private String path = "";
    private String pythonExecutable = "python3";
    private String launcherScript = "scripts/local_model_inference.py";
    private double temperature = 0.2;
    private int maxTokens = 128;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPythonExecutable() {
        return pythonExecutable;
    }

    public void setPythonExecutable(String pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
    }

    public String getLauncherScript() {
        return launcherScript;
    }

    public void setLauncherScript(String launcherScript) {
        this.launcherScript = launcherScript;
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
