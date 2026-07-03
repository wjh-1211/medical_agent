package com.medicalagent.config;

public class MemoryConfig {

    private String sessionStore = "redis";
    private int sessionTtlMinutes = 30;
    private String longTermStore = "mem0";
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

    public String getPreferenceStore() {
        return preferenceStore;
    }

    public void setPreferenceStore(String preferenceStore) {
        this.preferenceStore = preferenceStore;
    }
}
