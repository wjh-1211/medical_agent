package com.medicalagent.config;

public class PromptConfig {

    private String directory = "prompts";
    private String defaultTemplate = "medical-agent-react";
    private String fileExtension = ".prompt.md";
    private boolean strictVariables = true;

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(String defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public boolean isStrictVariables() {
        return strictVariables;
    }

    public void setStrictVariables(boolean strictVariables) {
        this.strictVariables = strictVariables;
    }
}
