package com.netsentinel.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Desktop Activity Log Model for Event Explorer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DesktopActivityLog {
    private Long id;
    private Long actorId;
    private String actorUsername;
    private String actorRole;
    private String action;
    private String targetResource;
    private String targetType;
    private String targetId;
    private String outcome;
    private String ipAddress;
    private String details;
    private String timestamp;

    public DesktopActivityLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetResource() { return targetResource; }
    public void setTargetResource(String targetResource) { this.targetResource = targetResource; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s (%s) -> %s | %s", timestamp, outcome, actorUsername, actorRole, action, targetResource);
    }
}

