package com.medicalagent.config;

public class GuardrailConfig {

    private boolean emergencyDetectionEnabled = true;
    private boolean riskAssessmentEnabled = true;
    private boolean groundingCheckEnabled = true;
    private boolean autoDisclaimerEnabled = true;

    public boolean isEmergencyDetectionEnabled() {
        return emergencyDetectionEnabled;
    }

    public void setEmergencyDetectionEnabled(boolean emergencyDetectionEnabled) {
        this.emergencyDetectionEnabled = emergencyDetectionEnabled;
    }

    public boolean isRiskAssessmentEnabled() {
        return riskAssessmentEnabled;
    }

    public void setRiskAssessmentEnabled(boolean riskAssessmentEnabled) {
        this.riskAssessmentEnabled = riskAssessmentEnabled;
    }

    public boolean isGroundingCheckEnabled() {
        return groundingCheckEnabled;
    }

    public void setGroundingCheckEnabled(boolean groundingCheckEnabled) {
        this.groundingCheckEnabled = groundingCheckEnabled;
    }

    public boolean isAutoDisclaimerEnabled() {
        return autoDisclaimerEnabled;
    }

    public void setAutoDisclaimerEnabled(boolean autoDisclaimerEnabled) {
        this.autoDisclaimerEnabled = autoDisclaimerEnabled;
    }
}
