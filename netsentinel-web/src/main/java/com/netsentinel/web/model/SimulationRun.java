package com.netsentinel.web.model;

import java.time.LocalDateTime;

public class SimulationRun {
    private Long id;
    private String simulationType;
    private String triggerPayload;
    private boolean detected;
    private Long incidentId;
    private LocalDateTime runAt;
    private String summary;

    public SimulationRun() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSimulationType() { return simulationType; }
    public void setSimulationType(String simulationType) { this.simulationType = simulationType; }

    public String getTriggerPayload() { return triggerPayload; }
    public void setTriggerPayload(String triggerPayload) { this.triggerPayload = triggerPayload; }

    public boolean isDetected() { return detected; }
    public void setDetected(boolean detected) { this.detected = detected; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public LocalDateTime getRunAt() { return runAt; }
    public void setRunAt(LocalDateTime runAt) { this.runAt = runAt; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}

