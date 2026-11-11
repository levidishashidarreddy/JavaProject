package com.netsentinel.web.model;

import java.time.LocalDateTime;

public class DetectionRuleConfig {
    private Long id;
    private String ruleCode;
    private String ruleName;
    private String category;
    private String description;
    private boolean enabled;
    private Integer threshold;
    private Integer timeWindowSeconds;
    private String severity;
    private LocalDateTime updatedAt;

    public DetectionRuleConfig() {}

    public DetectionRuleConfig(String ruleCode, String ruleName, String category, String severity, boolean enabled, Integer threshold, Integer timeWindowSeconds, String description) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.category = category;
        this.severity = severity;
        this.enabled = enabled;
        this.threshold = threshold;
        this.timeWindowSeconds = timeWindowSeconds;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }

    public Integer getTimeWindowSeconds() { return timeWindowSeconds; }
    public void setTimeWindowSeconds(Integer timeWindowSeconds) { this.timeWindowSeconds = timeWindowSeconds; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

