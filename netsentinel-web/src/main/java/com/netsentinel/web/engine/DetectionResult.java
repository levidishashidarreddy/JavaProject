package com.netsentinel.web.engine;

public class DetectionResult {
    private boolean detected;
    private String ruleCode;
    private String title;
    private String category;
    private String severity;
    private int confidenceScore;
    private String reason;
    private String evidenceJson;
    private String insightExplanation;
    private String recommendedAction;

    public static DetectionResult safe() {
        DetectionResult res = new DetectionResult();
        res.setDetected(false);
        return res;
    }

    public static DetectionResult detected(String ruleCode, String title, String category, String severity, 
                                           int confidenceScore, String reason, String evidenceJson, 
                                           String insightExplanation, String recommendedAction) {
        DetectionResult res = new DetectionResult();
        res.setDetected(true);
        res.setRuleCode(ruleCode);
        res.setTitle(title);
        res.setCategory(category);
        res.setSeverity(severity);
        res.setConfidenceScore(confidenceScore);
        res.setReason(reason);
        res.setEvidenceJson(evidenceJson);
        res.setInsightExplanation(insightExplanation);
        res.setRecommendedAction(recommendedAction);
        return res;
    }

    public boolean isDetected() { return detected; }
    public void setDetected(boolean detected) { this.detected = detected; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public int getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }

    public String getInsightExplanation() { return insightExplanation; }
    public void setInsightExplanation(String insightExplanation) { this.insightExplanation = insightExplanation; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
}

