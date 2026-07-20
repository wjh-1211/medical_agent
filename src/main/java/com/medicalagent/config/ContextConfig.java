package com.medicalagent.config;

public class ContextConfig {

    private boolean summaryEnabled = true;
    private int summaryUpdateMinHistoryMessages = 4;
    private int longTermMemoryMaxCharacters = 1200;
    private int sessionMemoryMaxCharacters = 800;
    private int summaryMemoryMaxCharacters = 1200;
    private int recentHistoryMaxMessages = 4;
    private int recentHistoryMaxCharacters = 1800;
    private int toolFactsMaxCharacters = 1000;
    private int observationsMaxMessages = 2;
    private int observationsMaxCharacters = 1000;

    public boolean isSummaryEnabled() {
        return summaryEnabled;
    }

    public void setSummaryEnabled(boolean summaryEnabled) {
        this.summaryEnabled = summaryEnabled;
    }

    public int getSummaryUpdateMinHistoryMessages() {
        return summaryUpdateMinHistoryMessages;
    }

    public void setSummaryUpdateMinHistoryMessages(int summaryUpdateMinHistoryMessages) {
        this.summaryUpdateMinHistoryMessages = summaryUpdateMinHistoryMessages;
    }

    public int getLongTermMemoryMaxCharacters() {
        return longTermMemoryMaxCharacters;
    }

    public void setLongTermMemoryMaxCharacters(int longTermMemoryMaxCharacters) {
        this.longTermMemoryMaxCharacters = longTermMemoryMaxCharacters;
    }

    public int getSessionMemoryMaxCharacters() {
        return sessionMemoryMaxCharacters;
    }

    public void setSessionMemoryMaxCharacters(int sessionMemoryMaxCharacters) {
        this.sessionMemoryMaxCharacters = sessionMemoryMaxCharacters;
    }

    public int getSummaryMemoryMaxCharacters() {
        return summaryMemoryMaxCharacters;
    }

    public void setSummaryMemoryMaxCharacters(int summaryMemoryMaxCharacters) {
        this.summaryMemoryMaxCharacters = summaryMemoryMaxCharacters;
    }

    public int getRecentHistoryMaxMessages() {
        return recentHistoryMaxMessages;
    }

    public void setRecentHistoryMaxMessages(int recentHistoryMaxMessages) {
        this.recentHistoryMaxMessages = recentHistoryMaxMessages;
    }

    public int getRecentHistoryMaxCharacters() {
        return recentHistoryMaxCharacters;
    }

    public void setRecentHistoryMaxCharacters(int recentHistoryMaxCharacters) {
        this.recentHistoryMaxCharacters = recentHistoryMaxCharacters;
    }

    public int getToolFactsMaxCharacters() {
        return toolFactsMaxCharacters;
    }

    public void setToolFactsMaxCharacters(int toolFactsMaxCharacters) {
        this.toolFactsMaxCharacters = toolFactsMaxCharacters;
    }

    public int getObservationsMaxMessages() {
        return observationsMaxMessages;
    }

    public void setObservationsMaxMessages(int observationsMaxMessages) {
        this.observationsMaxMessages = observationsMaxMessages;
    }

    public int getObservationsMaxCharacters() {
        return observationsMaxCharacters;
    }

    public void setObservationsMaxCharacters(int observationsMaxCharacters) {
        this.observationsMaxCharacters = observationsMaxCharacters;
    }
}
