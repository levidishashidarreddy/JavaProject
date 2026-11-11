package com.netsentinel.web.controller.api;

import com.netsentinel.web.model.ActivityLog;
import com.netsentinel.web.model.Incident;
import com.netsentinel.web.model.IncidentEvent;
import com.netsentinel.web.model.SimulationResponse;
import com.netsentinel.web.repository.ActivityLogRepository;
import com.netsentinel.web.service.AnalyticsService;
import com.netsentinel.web.service.IncidentService;
import com.netsentinel.web.service.SimulationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/desktop")
@CrossOrigin(origins = "*")
public class DesktopSyncApiController {

    private final AnalyticsService analyticsService;
    private final IncidentService incidentService;
    private final SimulationService simulationService;
    private final ActivityLogRepository activityLogRepository;

    public DesktopSyncApiController(AnalyticsService analyticsService,
                                   IncidentService incidentService,
                                   SimulationService simulationService,
                                   ActivityLogRepository activityLogRepository) {
        this.analyticsService = analyticsService;
        this.incidentService = incidentService;
        this.simulationService = simulationService;
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "UP");
        res.put("service", "NetSentinel Security Core Engine");
        res.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(analyticsService.getDashboardMetrics());
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<Incident>> getIncidents(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(incidentService.getRecentIncidents(limit));
    }

    @GetMapping("/incidents/{id}/events")
    public ResponseEntity<List<IncidentEvent>> getIncidentEvents(@PathVariable("id") Long id) {
        return ResponseEntity.ok(incidentService.getIncidentEvents(id));
    }

    @GetMapping("/activity-logs")
    public ResponseEntity<List<ActivityLog>> getActivityLogs(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return ResponseEntity.ok(activityLogRepository.findRecent(limit));
    }

    @PostMapping("/simulate")
    public ResponseEntity<SimulationResponse> runSimulation(@RequestParam(value = "type", defaultValue = "UNAUTHORIZED_ATTENDANCE") String type,
                                                            @RequestParam(value = "actor", defaultValue = "student101") String actor,
                                                            HttpServletRequest request) {
        SimulationResponse resp = simulationService.runSimulation(type, actor, request.getRemoteAddr());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetDemo() {
        simulationService.resetDemoData();
        return ResponseEntity.ok(Map.of("success", true, "message", "Demo state reset successfully"));
    }
}

