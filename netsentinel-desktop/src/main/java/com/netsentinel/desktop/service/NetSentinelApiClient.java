package com.netsentinel.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netsentinel.desktop.model.DesktopActivityLog;
import com.netsentinel.desktop.model.DesktopEvent;
import com.netsentinel.desktop.model.DesktopIncident;
import com.netsentinel.desktop.model.DesktopStats;
import com.netsentinel.desktop.model.SimulationResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class NetSentinelApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public NetSentinelApiClient() {
        this("http://localhost:8080");
    }

    public NetSentinelApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean checkHealth() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/desktop/health"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public DesktopStats fetchStats() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/desktop/stats"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), DesktopStats.class);
            }
        } catch (Exception e) {
            System.err.println("Error fetching stats from NetSentinel API: " + e.getMessage());
        }
        return new DesktopStats();
    }

    public List<DesktopIncident> fetchIncidents() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/desktop/incidents?limit=50"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), new TypeReference<List<DesktopIncident>>() {});
            }
        } catch (Exception e) {
            System.err.println("Error fetching incidents from NetSentinel API: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<DesktopEvent> fetchIncidentEvents(Long incidentId) {
        if (incidentId == null) return Collections.emptyList();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/desktop/incidents/" + incidentId + "/events"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), new TypeReference<List<DesktopEvent>>() {});
            }
        } catch (Exception e) {
            System.err.println("Error fetching incident events: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<DesktopActivityLog> fetchActivityLogs(int limit) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/desktop/activity-logs?limit=" + limit))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), new TypeReference<List<DesktopActivityLog>>() {});
            }
        } catch (Exception e) {
            System.err.println("Error fetching activity logs: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public boolean resetDemoData() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/desktop/reset"))
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error triggering demo reset: " + e.getMessage());
            return false;
        }
    }

    public SimulationResult triggerSimulation(String type, String actor) {
        try {
            String form = "type=" + java.net.URLEncoder.encode(type, java.nio.charset.StandardCharsets.UTF_8)
                    + "&actor=" + java.net.URLEncoder.encode(actor, java.nio.charset.StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/desktop/simulate"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readValue(resp.body(), SimulationResult.class);
            }
        } catch (Exception e) {
            System.err.println("Error executing simulation: " + e.getMessage());
        }
        return null;
    }
}

