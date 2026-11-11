package com.netsentinel.web.engine;

import java.util.HashMap;
import java.util.Map;

public class DetectionContext {
    private Long actorId;
    private String actorUsername;
    private String actorRole;
    private String action;
    private String targetResource;
    private String ipAddress;
    private String userAgent;
    private Long subjectId;
    private String subjectCode;
    private Long studentId;
    private String targetType;
    private String targetId;
    private String requestId;
    private Map<String, Object> attributes = new HashMap<>();

    public DetectionContext() {}

    public DetectionContext(Long actorId, String actorUsername, String actorRole, String action, String targetResource, String ipAddress) {
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.targetResource = targetResource;
        this.ipAddress = ipAddress;
    }

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

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }

    public void addAttribute(String key, Object val) {
        this.attributes.put(key, val);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }
}

