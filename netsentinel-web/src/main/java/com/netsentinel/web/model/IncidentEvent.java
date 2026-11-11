package com.netsentinel.web.model;

import java.time.LocalDateTime;

public class IncidentEvent {
    private Long id;
    private Long incidentId;
    private String eventType;
    private Long actorId;
    private String actorUsername;
    private String notes;
    private LocalDateTime timestamp;

    public IncidentEvent() {}

    public IncidentEvent(Long incidentId, String eventType, Long actorId, String actorUsername, String notes, LocalDateTime timestamp) {
        this.incidentId = incidentId;
        this.eventType = eventType;
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.notes = notes;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

