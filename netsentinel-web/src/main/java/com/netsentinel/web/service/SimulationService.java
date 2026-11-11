package com.netsentinel.web.service;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.SecurityDetectionEngine;
import com.netsentinel.web.model.Incident;
import com.netsentinel.web.model.SimulationResponse;
import com.netsentinel.web.model.SimulationRun;
import com.netsentinel.web.repository.SimulationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing threat scenario execution in the Attack Simulation Lab.
 * 
 * Demonstrates:
 * - Deterministic attack vector reproduction
 * - Controlled demo reset function for administrators
 * - Real Java security service execution without frontend mockups
 */
@Service
public class SimulationService {

    private final SecurityDetectionEngine detectionEngine;
    private final SimulationRepository simulationRepository;
    private final ActivityLogService activityLogService;
    private final JdbcTemplate jdbcTemplate;

    public SimulationService(SecurityDetectionEngine detectionEngine,
                             SimulationRepository simulationRepository,
                             ActivityLogService activityLogService,
                             JdbcTemplate jdbcTemplate) {
        this.detectionEngine = detectionEngine;
        this.simulationRepository = simulationRepository;
        this.activityLogService = activityLogService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public SimulationResponse runSimulation(String type, String simulatedActor, String clientIp) {
        if (type == null) type = "UNAUTHORIZED_ATTENDANCE";
        if (simulatedActor == null || simulatedActor.isBlank()) simulatedActor = "student101";
        if (clientIp == null || clientIp.isBlank()) clientIp = "192.168.1.188";

        DetectionContext ctx = new DetectionContext();
        ctx.setIpAddress(clientIp);
        ctx.setActorUsername(simulatedActor);

        Map<String, Object> attrs = new HashMap<>();

        switch (type.toUpperCase()) {
            case "UNAUTHORIZED_ATTENDANCE" -> {
                ctx.setActorId(1L);
                ctx.setActorRole("STUDENT");
                ctx.setAction("ATTENDANCE_MUTATE_ATTEMPT");
                ctx.setTargetResource("/faculty/attendance/record");
                ctx.setSubjectId(1L);
                ctx.setSubjectCode("CS301");
                ctx.setStudentId(1L);
                attrs.put("simulation_unauthorized_attendance", true);
            }
            case "BULK_ACADEMIC_MODIFICATION", "BULK_MODIFICATION" -> {
                ctx.setActorId(3L);
                ctx.setActorUsername(simulatedActor.equals("student101") ? "faculty102" : simulatedActor);
                ctx.setActorRole("FACULTY");
                ctx.setAction("BULK_MARKS_UPDATE");
                ctx.setTargetResource("/faculty/marks/record");
                attrs.put("simulation_bulk_modification", true);
            }
            case "UNAUTHORIZED_PRIVILEGE_CHANGE", "PRIVILEGE_CHANGE" -> {
                ctx.setActorId(1L);
                ctx.setActorRole("STUDENT");
                ctx.setAction("USER_ROLE_CHANGE");
                ctx.setTargetResource("/admin/users/1/role");
                attrs.put("simulation_privilege_escalation", true);
                attrs.put("target_user", simulatedActor);
                attrs.put("old_role", "STUDENT");
                attrs.put("requested_role", "ADMIN");
            }
            case "UNAUTHORIZED_STUDENT_RECORD_ACCESS", "RECORD_HARVESTING", "RECORD_ACCESS" -> {
                ctx.setActorId(1L);
                ctx.setActorUsername("student101");
                ctx.setActorRole("STUDENT");
                ctx.setAction("UNAUTHORIZED_STUDENT_RECORD_ACCESS");
                ctx.setTargetResource("/student/marks?studentId=2");
                ctx.setStudentId(2L); // Attempting to access Student 102
                attrs.put("simulation_unauthorized_student_access", true);
                attrs.put("target_student_id", 2L);
            }
            default -> {
                ctx.setActorId(1L);
                ctx.setActorRole("STUDENT");
                ctx.setAction("ATTENDANCE_MUTATE_ATTEMPT");
                ctx.setTargetResource("/faculty/attendance/record");
                ctx.setSubjectId(1L);
                ctx.setSubjectCode("CS301");
                ctx.setStudentId(1L);
                attrs.put("simulation_unauthorized_attendance", true);
            }
        }

        ctx.setAttributes(attrs);

        // 1. Audit log the simulated event
        activityLogService.log(ctx.getActorId(), ctx.getActorUsername(), ctx.getActorRole(),
                ctx.getAction(), "SIMULATION", type, ctx.getTargetResource(), "BLOCKED", ctx.getIpAddress(),
                "Simulation Lab Runner", "Simulated cyber threat vector executed in sandboxed demonstration lab.",
                java.util.UUID.randomUUID().toString(), attrs);

        // 2. Evaluate in Security Detection Engine
        Optional<Incident> incidentOpt = detectionEngine.evaluate(ctx);

        SimulationResponse resp = new SimulationResponse();
        resp.setSimulationType(type);
        resp.setRunAt(LocalDateTime.now());

        if (incidentOpt.isPresent()) {
            Incident inc = incidentOpt.get();
            resp.setDetected(true);
            resp.setIncidentId(inc.getId());
            resp.setIncidentNumber(inc.getIncidentNumber());
            resp.setRuleCode(inc.getRuleCode());
            resp.setSeverity(inc.getSeverity());
            resp.setConfidenceScore(inc.getConfidenceScore());
            resp.setTitle(inc.getTitle());
            resp.setReason(inc.getReason());
            resp.setEvidenceJson(inc.getEvidenceJson());
            resp.setInsightExplanation(inc.getInsightExplanation());
            resp.setRecommendedAction(inc.getRecommendedAction());

            // 3. Record Simulation Run
            SimulationRun run = new SimulationRun();
            run.setSimulationType(type);
            run.setTriggerPayload(inc.getEvidenceJson());
            run.setDetected(true);
            run.setIncidentId(inc.getId());
            run.setRunAt(LocalDateTime.now());
            run.setSummary("Triggered rule " + inc.getRuleCode() + " -> " + inc.getTitle() + " (" + inc.getSeverity() + ")");
            simulationRepository.recordRun(run);
        } else {
            resp.setDetected(false);
            resp.setTitle("No Incident Triggered");
            resp.setReason("Simulation payload was evaluated as non-anomalous or rule is disabled.");

            SimulationRun run = new SimulationRun();
            run.setSimulationType(type);
            run.setTriggerPayload("{\"type\": \"" + type + "\"}");
            run.setDetected(false);
            run.setRunAt(LocalDateTime.now());
            run.setSummary("Simulation executed without triggering rule.");
            simulationRepository.recordRun(run);
        }

        return resp;
    }

    public List<SimulationRun> getRecentSimulationRuns(int limit) {
        return simulationRepository.findRecentRuns(limit);
    }

    /**
     * Controlled Demo Reset Function for Administrators.
     * Resets simulation runs, purges ephemeral test incidents, and resets user roles.
     */
    @Transactional
    public void resetDemoData() {
        simulationRepository.clearAll();
        // Remove simulation-generated incidents and events
        jdbcTemplate.update("DELETE FROM incident_events");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM incidents");
        
        // Reset user roles back to canonical defaults
        jdbcTemplate.update("UPDATE users SET role = 'STUDENT', status = 'ACTIVE' WHERE username IN ('student101', 'student102')");
        jdbcTemplate.update("UPDATE users SET role = 'FACULTY', status = 'ACTIVE' WHERE username IN ('faculty101', 'faculty102')");
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN', status = 'ACTIVE' WHERE username = 'admin01'");
        
        // Clean up any legacy alias accounts
        jdbcTemplate.update("DELETE FROM users WHERE LOWER(username) IN ('student', 'faculty_alan', 'faculty_sarah', 'faculty_a', 'faculty_b', 'admin')");

        activityLogService.logAllowed(5L, "admin01", "ADMIN", "DEMO_STATE_RESET",
                "SIMULATION_LAB", "ALL", "/admin/simulation/reset", "127.0.0.1",
                "Administrator executed controlled demo environment reset.");
    }
}
