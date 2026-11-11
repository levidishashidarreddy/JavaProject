package com.netsentinel.web.engine.insight;

import java.util.List;
import java.util.Map;

/**
 * Explainable Intelligence Dossier containing the 8 structured dimensions
 * required by the Sentinel Insight Engine.
 * 
 * Demonstrates:
 * - Clean object encapsulation
 * - Structured XAI (Explainable AI / Explainable Heuristics)
 * - Clear separation of observed facts from interpretations
 */
public class ExplainableDossier {

    private String incidentNumber;
    private String title;
    private String ruleCode;
    private String ruleName;
    private String severity;
    private String status;
    private String timestamp;

    // The 8 Core Explainability Dimensions
    private String whatHappened;
    private String whoPerformedAction;
    private String resourceTargeted;
    private String whyFlagged;
    private String ruleTriggeredDetails;
    private Map<String, Object> evidenceDetails;
    private String potentialImpact;
    private List<String> recommendedNextSteps;

    // Forensic Activity Context
    private List<Map<String, String>> relatedActivityLogs;

    public ExplainableDossier() {}

    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getWhatHappened() { return whatHappened; }
    public void setWhatHappened(String whatHappened) { this.whatHappened = whatHappened; }

    public String getWhoPerformedAction() { return whoPerformedAction; }
    public void setWhoPerformedAction(String whoPerformedAction) { this.whoPerformedAction = whoPerformedAction; }

    public String getResourceTargeted() { return resourceTargeted; }
    public void setResourceTargeted(String resourceTargeted) { this.resourceTargeted = resourceTargeted; }

    public String getWhyFlagged() { return whyFlagged; }
    public void setWhyFlagged(String whyFlagged) { this.whyFlagged = whyFlagged; }

    public String getRuleTriggeredDetails() { return ruleTriggeredDetails; }
    public void setRuleTriggeredDetails(String ruleTriggeredDetails) { this.ruleTriggeredDetails = ruleTriggeredDetails; }

    public Map<String, Object> getEvidenceDetails() { return evidenceDetails; }
    public void setEvidenceDetails(Map<String, Object> evidenceDetails) { this.evidenceDetails = evidenceDetails; }

    public String getPotentialImpact() { return potentialImpact; }
    public void setPotentialImpact(String potentialImpact) { this.potentialImpact = potentialImpact; }

    public List<String> getRecommendedNextSteps() { return recommendedNextSteps; }
    public void setRecommendedNextSteps(List<String> recommendedNextSteps) { this.recommendedNextSteps = recommendedNextSteps; }

    public List<Map<String, String>> getRelatedActivityLogs() { return relatedActivityLogs; }
    public void setRelatedActivityLogs(List<Map<String, String>> relatedActivityLogs) { this.relatedActivityLogs = relatedActivityLogs; }
}

