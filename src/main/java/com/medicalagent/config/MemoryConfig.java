package com.medicalagent.config;

public class MemoryConfig {

    private String sessionStore = "redis";
    private int sessionTtlMinutes = 30;
    private String longTermStore = "sqlite";
    private String longTermSqlitePath = "data/medical-agent.db";
    private String preferenceStore = "mysql";

    public String getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(String sessionStore) {
        this.sessionStore = sessionStore;
    }

    public int getSessionTtlMinutes() {
        return sessionTtlMinutes;
    }

    public void setSessionTtlMinutes(int sessionTtlMinutes) {
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    public String getLongTermStore() {
        return longTermStore;
    }

    public void setLongTermStore(String longTermStore) {
        this.longTermStore = longTermStore;
    }

    public String getLongTermSqlitePath() {
        return longTermSqlitePath;
    }

    public void setLongTermSqlitePath(String longTermSqlitePath) {
        this.longTermSqlitePath = longTermSqlitePath;
    }

    public String getPreferenceStore() {
        return preferenceStore;
    }

    public void setPreferenceStore(String preferenceStore) {
        this.preferenceStore = preferenceStore;
    }
}
