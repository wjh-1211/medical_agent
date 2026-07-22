package com.medicalagent.config;

public class GuardrailConfig {

    private boolean emergencyDetectionEnabled = true;
    private boolean riskAssessmentEnabled = true;
    private boolean groundingCheckEnabled = true;
    private boolean autoDisclaimerEnabled = true;
    private boolean blockOnDecisionFailure = true;
    private String emergencySafeAnswer = "当前描述可能存在紧急风险。请尽快联系当地急救服务或立即前往急诊；如身边有人，请让对方协助，不要独自驾驶。";
    private String highRiskSafeAnswer = "当前信息提示需要尽快进行线下医疗评估。为避免延误或不恰当的个体化建议，请联系医生、药师或就近医疗机构进一步确认。";
    private String insufficientEvidenceSafeAnswer = "当前可用信息或受控资料不足以支持确定性结论。请补充症状变化、持续时间、既往病史和正在使用的药物；如症状加重或出现危险信号，请及时就医。";
    private String decisionFailureSafeAnswer = "当前无法完成必要的安全评估，因此不适合给出确定性医疗建议。请联系医生、药师或就近医疗机构进一步评估；如症状紧急，请立即寻求急救帮助。";

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

    public boolean isBlockOnDecisionFailure() {
        return blockOnDecisionFailure;
    }

    public void setBlockOnDecisionFailure(boolean blockOnDecisionFailure) {
        this.blockOnDecisionFailure = blockOnDecisionFailure;
    }

    public String getEmergencySafeAnswer() {
        return emergencySafeAnswer;
    }

    public void setEmergencySafeAnswer(String emergencySafeAnswer) {
        this.emergencySafeAnswer = emergencySafeAnswer;
    }

    public String getHighRiskSafeAnswer() {
        return highRiskSafeAnswer;
    }

    public void setHighRiskSafeAnswer(String highRiskSafeAnswer) {
        this.highRiskSafeAnswer = highRiskSafeAnswer;
    }

    public String getInsufficientEvidenceSafeAnswer() {
        return insufficientEvidenceSafeAnswer;
    }

    public void setInsufficientEvidenceSafeAnswer(String insufficientEvidenceSafeAnswer) {
        this.insufficientEvidenceSafeAnswer = insufficientEvidenceSafeAnswer;
    }

    public String getDecisionFailureSafeAnswer() {
        return decisionFailureSafeAnswer;
    }

    public void setDecisionFailureSafeAnswer(String decisionFailureSafeAnswer) {
        this.decisionFailureSafeAnswer = decisionFailureSafeAnswer;
    }
}
