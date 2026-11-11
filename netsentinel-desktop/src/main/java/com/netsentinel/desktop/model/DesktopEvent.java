package com.netsentinel.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Desktop Incident Timeline Event Model.
 * Represents chronological detection and investigation events.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DesktopEvent {
    private Long id;
    private Long incidentId;
    private String eventType;
    private Long actorId;
    private String actorUsername;
    private String notes;
    private String timestamp;

    public DesktopEvent() {}

    public DesktopEvent(Long id, Long incidentId, String eventType, String actorUsername, String notes, String timestamp) {
        this.id = id;
        this.incidentId = incidentId;
        this.eventType = eventType;
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

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s — %s (%s)", timestamp != null ? timestamp : "LIVE", eventType, notes, actorUsername != null ? actorUsername : "SYSTEM");
    }
}

