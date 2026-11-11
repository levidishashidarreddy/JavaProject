package com.netsentinel.web.model;

import java.time.LocalDateTime;

/**
 * Audit and Security Activity Log Entity.
 * 
 * Demonstrates:
 * - Encapsulation with private fields and getters/setters
 * - Multiple overloaded constructors
 * - Immutable identifiers and UTC timestamp recording
 */
public class ActivityLog {

    private Long id;
    private Long actorId;
    private String actorUsername;
    private String actorRole;
    private String action;
    private String targetResource;
    private String targetType; // e.g. "STUDENT_RECORD", "ATTENDANCE", "SUBJECT", "USER_ROLE"
    private String targetId;   // e.g. "STU-042", "CS301", "2"
    private String requestId;  // UUID correlation trace ID
    private String outcome;    // SUCCESS, DENIED, BLOCKED, FLAGGED, ALLOWED
    private String ipAddress;
    private String userAgent;
    private String details;
    private LocalDateTime timestamp;

    public ActivityLog() {
        this.timestamp = LocalDateTime.now();
    }

    public ActivityLog(Long actorId, String actorUsername, String actorRole, String action, 
                       String targetResource, String outcome, String ipAddress, String userAgent, 
                       String details, LocalDateTime timestamp) {
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.targetResource = targetResource;
        this.targetType = "RESOURCE";
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.details = details;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public ActivityLog(Long actorId, String actorUsername, String actorRole, String action, 
                       String targetType, String targetId, String targetResource, 
                       String outcome, String ipAddress, String userAgent, 
                       String details, String requestId) {
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetResource = targetResource;
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.details = details;
        this.requestId = requestId;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getUsername() { return actorUsername; }
    public void setUsername(String username) { this.actorUsername = username; }

    public String getResource() { return targetResource; }
    public void setResource(String resource) { this.targetResource = resource; }

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

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
