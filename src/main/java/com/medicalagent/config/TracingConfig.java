package com.medicalagent.config;

public class TracingConfig {

    private boolean enabled = true;
    private int maxPayloadCharacters = 320;
    private long slowCallMillis = 1000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxPayloadCharacters() {
        return maxPayloadCharacters;
    }

    public void setMaxPayloadCharacters(int maxPayloadCharacters) {
        this.maxPayloadCharacters = maxPayloadCharacters;
    }

    public long getSlowCallMillis() {
        return slowCallMillis;
    }

    public void setSlowCallMillis(long slowCallMillis) {
        this.slowCallMillis = slowCallMillis;
    }
}
