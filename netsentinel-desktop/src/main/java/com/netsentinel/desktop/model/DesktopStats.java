package com.netsentinel.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DesktopStats {
    private int totalActivities;
    private int totalIncidents;
    private int highCriticalIncidents;
    private int openIncidents;
    private int investigatingIncidents;
    private int resolvedIncidents;
    private int threatIndex;
    private int healthScore = 95;
    private String healthStatus = "EXCELLENT";

    public DesktopStats() {}

    public int getTotalActivities() { return totalActivities; }
    public void setTotalActivities(int totalActivities) { this.totalActivities = totalActivities; }

    public int getTotalIncidents() { return totalIncidents; }
    public void setTotalIncidents(int totalIncidents) { this.totalIncidents = totalIncidents; }

    public int getHighCriticalIncidents() { return highCriticalIncidents; }
    public void setHighCriticalIncidents(int highCriticalIncidents) { this.highCriticalIncidents = highCriticalIncidents; }

    public int getOpenIncidents() { return openIncidents; }
    public void setOpenIncidents(int openIncidents) { this.openIncidents = openIncidents; }

    public int getInvestigatingIncidents() { return investigatingIncidents; }
    public void setInvestigatingIncidents(int investigatingIncidents) { this.investigatingIncidents = investigatingIncidents; }

    public int getResolvedIncidents() { return resolvedIncidents; }
    public void setResolvedIncidents(int resolvedIncidents) { this.resolvedIncidents = resolvedIncidents; }

    public int getThreatIndex() { return threatIndex; }
    public void setThreatIndex(int threatIndex) { this.threatIndex = threatIndex; }

    public int getHealthScore() { return healthScore; }
    public void setHealthScore(int healthScore) { this.healthScore = healthScore; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
}

