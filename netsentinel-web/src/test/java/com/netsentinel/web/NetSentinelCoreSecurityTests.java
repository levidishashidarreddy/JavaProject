package com.netsentinel.web;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.DetectionResult;
import com.netsentinel.web.engine.insight.ExplainableDossier;
import com.netsentinel.web.engine.insight.SentinelInsightService;
import com.netsentinel.web.engine.rules.BulkAcademicModificationRule;
import com.netsentinel.web.exception.SecurityViolationException;
import com.netsentinel.web.exception.UnauthorizedAccessException;
import com.netsentinel.web.model.*;
import com.netsentinel.web.repository.*;
import com.netsentinel.web.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class NetSentinelCoreSecurityTests {

    @Autowired
    private AcademicService academicService;

    @Autowired
    private UserService userService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private PolicyTestingService policyTestingService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private SentinelInsightService sentinelInsightService;

    @Autowired
    private ReportExportService reportExportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private BulkAcademicModificationRule bulkAcademicModificationRule;

    private User student101;
    private User student102;
    private User faculty101;
    private User faculty102;
    private User admin01;
    private Subject cs301;
    private Subject cs305;

    @BeforeEach
    void setUp() {
        student101 = userRepository.findByUsername("student101").orElseThrow();
        student102 = userRepository.findByUsername("student102").orElseThrow();
        faculty101 = userRepository.findByUsername("faculty101").orElseThrow();
        faculty102 = userRepository.findByUsername("faculty102").orElseThrow();
        admin01 = userRepository.findByUsername("admin01").orElseThrow();

        cs301 = subjectRepository.findByCode("CS301").orElseThrow();
        cs305 = subjectRepository.findByCode("CS305").orElseThrow();
    }

    // =========================================================================
    // TEST 1: Student accessing own marks (allowed)
    // =========================================================================
    @Test
    @DisplayName("Test 1: Student 101 accessing own marks is ALLOWED")
    void test1_studentAccessingOwnMarksAllowed() {
        List<Mark> marks = academicService.getStudentMarksSecure(
                student101.getId(), student101.getUsername(), student101.getRole().name(),
                student101.getId(), "127.0.0.1"
        );

        assertNotNull(marks, "Marks list should not be null");
        assertFalse(marks.isEmpty(), "Student 101 should have seeded marks in CS301");
        for (Mark m : marks) {
            assertEquals(student101.getId(), m.getStudentId(), "All returned marks must belong to student 101");
        }
    }

    // =========================================================================
    // TEST 2: Student accessing another student's marks (blocked + logged)
    // =========================================================================
    @Test
    @DisplayName("Test 2: Student 101 accessing Student 102 marks is BLOCKED and logged (RULE-REC-004)")
    void test2_studentAccessingAnotherStudentMarksBlocked() {
        assertThrows(UnauthorizedAccessException.class, () -> {
            academicService.getStudentMarksSecure(
                    student101.getId(), student101.getUsername(), student101.getRole().name(),
                    student102.getId(), "127.0.0.1"
            );
        }, "Horizontal student record access must throw UnauthorizedAccessException");

        // Verify audit log captured denied outcome
        List<ActivityLog> recentLogs = activityLogRepository.findRecent(10);
        boolean foundDenied = recentLogs.stream().anyMatch(log ->
                "student101".equalsIgnoreCase(log.getActorUsername()) &&
                "DENIED".equalsIgnoreCase(log.getOutcome()) &&
                ("UNAUTHORIZED_STUDENT_RECORD_ACCESS".equalsIgnoreCase(log.getAction()) || log.getTargetResource().contains("/student/marks"))
        );
        assertTrue(foundDenied, "Activity log repository must record DENIED record for horizontal IDOR attempt");
    }

    // =========================================================================
    // TEST 3: Faculty updating assigned subject attendance (allowed)
    // =========================================================================
    @Test
    @DisplayName("Test 3: Faculty 101 recording attendance for assigned subject CS301 is ALLOWED")
    void test3_facultyUpdatingAssignedSubjectAttendanceAllowed() {
        // Faculty 101 teaches CS301; Student 101 is enrolled in CS301
        boolean success = academicService.recordAttendance(
                faculty101.getId(), faculty101.getUsername(),
                student101.getId(), cs301.getId(),
                LocalDate.now(), "PRESENT", "On-schedule lecture verify", "127.0.0.1"
        );

        assertTrue(success, "Faculty 101 must be permitted to record attendance for assigned course CS301");
    }

    // =========================================================================
    // TEST 4: Faculty updating unassigned subject attendance (blocked + incident created)
    // =========================================================================
    @Test
    @DisplayName("Test 4: Faculty 102 recording attendance for unassigned subject CS301 is BLOCKED (RULE-ATT-001)")
    void test4_facultyUpdatingUnassignedSubjectAttendanceBlockedAndIncidentCreated() {
        int incidentsBefore = incidentRepository.countTotal();

        // Faculty 102 is NOT assigned to CS301 (Faculty 102 teaches CS305 & CS405)
        boolean success = academicService.recordAttendance(
                faculty102.getId(), faculty102.getUsername(),
                student101.getId(), cs301.getId(),
                LocalDate.now(), "PRESENT", "Unauthorized tampering attempt", "192.168.1.100"
        );

        assertFalse(success, "Operation must return false / be rejected for unassigned faculty");

        int incidentsAfter = incidentRepository.countTotal();
        assertTrue(incidentsAfter > incidentsBefore, "An incident must be generated for unassigned course tampering");

        List<Incident> recentIncidents = incidentRepository.findRecent(20);
        boolean foundAttIncident = recentIncidents.stream().anyMatch(inc ->
                "RULE-ATT-001".equalsIgnoreCase(inc.getRuleCode()) &&
                "faculty102".equalsIgnoreCase(inc.getActorUsername())
        );
        assertTrue(foundAttIncident, "Recent incidents must contain RULE-ATT-001 for faculty102");
    }

    // =========================================================================
    // TEST 5: Normal academic modifications (allowed)
    // =========================================================================
    @Test
    @DisplayName("Test 5: Normal academic grade assignment under threshold is ALLOWED")
    void test5_normalAcademicModificationsAllowed() {
        boolean success = academicService.recordMark(
                faculty101.getId(), faculty101.getUsername(),
                student101.getId(), cs301.getId(),
                "QUIZ", 92.5, 100.0, "127.0.0.1"
        );

        assertTrue(success, "Normal mark submission by assigned faculty must be permitted");
    }

    // =========================================================================
    // TEST 6: Bulk modification threshold exceeded (triggers RULE-MOD-002)
    // =========================================================================
    @Test
    @DisplayName("Test 6: High-velocity bulk modification triggers RULE-MOD-002 anomaly")
    void test6_bulkModificationThresholdExceededTriggersRule() {
        // Evaluate bulk modification detection logic
        DetectionContext ctx = new DetectionContext(
                faculty101.getId(), faculty101.getUsername(), "FACULTY",
                "BULK_MARKS_UPDATE", "/api/v1/faculty/marks/bulk", "127.0.0.1"
        );
        ctx.addAttribute("simulation_bulk_modification", true);

        DetectionRuleConfig config = new DetectionRuleConfig(
                "RULE-MOD-002", "Bulk Modification", "INTEGRITY", "CRITICAL", true, 3, 60, "Alert on burst"
        );

        DetectionResult result = bulkAcademicModificationRule.evaluate(ctx, config);
        assertNotNull(result, "DetectionResult must not be null");
        assertTrue(result.isDetected(), "High-velocity bulk modification must be flagged as anomaly");
        assertEquals("RULE-MOD-002", result.getRuleCode(), "Rule code must match RULE-MOD-002");
    }

    // =========================================================================
    // TEST 7: Faculty attempting role escalation (blocked + incident created)
    // =========================================================================
    @Test
    @DisplayName("Test 7: Faculty attempting user role mutation is BLOCKED and creates RULE-PRV-003 incident")
    void test7_facultyAttemptingRoleEscalationBlocked() {
        int incidentsBefore = incidentRepository.countTotal();

        assertThrows(SecurityViolationException.class, () -> {
            userService.updateUserRole(
                    faculty101.getId(), faculty101.getUsername(), faculty101.getRole().name(),
                    faculty101.getId(), "ADMIN", "192.168.1.150"
            );
        }, "Non-admin actor modifying roles must throw SecurityViolationException");

        int incidentsAfter = incidentRepository.countTotal();
        assertTrue(incidentsAfter > incidentsBefore, "An incident must be generated for privilege escalation attempt");

        List<Incident> recentIncidents = incidentRepository.findRecent(20);
        boolean foundEscalation = recentIncidents.stream().anyMatch(inc ->
                "RULE-PRV-003".equalsIgnoreCase(inc.getRuleCode()) &&
                "faculty101".equalsIgnoreCase(inc.getActorUsername())
        );
        assertTrue(foundEscalation, "Recent incidents must contain RULE-PRV-003 for faculty101 privilege escalation");
    }

    // =========================================================================
    // TEST 8: Authorized admin changing a role (allowed + audit logged)
    // =========================================================================
    @Test
    @DisplayName("Test 8: Authorized Admin changing user role is ALLOWED and logged")
    void test8_authorizedAdminChangingRoleAllowedAndLogged() {
        boolean success = userService.updateUserRole(
                admin01.getId(), admin01.getUsername(), "ADMIN",
                student102.getId(), "STUDENT", "127.0.0.1"
        );

        assertTrue(success, "Administrator must be authorized to update user role");

        List<ActivityLog> recentLogs = activityLogRepository.findRecent(10);
        boolean foundLog = recentLogs.stream().anyMatch(l ->
                "admin01".equalsIgnoreCase(l.getActorUsername()) &&
                "USER_ROLE_CHANGE".equalsIgnoreCase(l.getAction()) &&
                ("ALLOWED".equalsIgnoreCase(l.getOutcome()) || "SUCCESS".equalsIgnoreCase(l.getOutcome()))
        );
        assertTrue(foundLog, "Admin role change must be recorded in activity audit log");
    }

    // =========================================================================
    // TEST 9: Incident creation with forensic evidence
    // =========================================================================
    @Test
    @DisplayName("Test 9: Incident creation records rich forensic evidence payload")
    void test9_incidentCreationWithEvidence() {
        List<Incident> incidents = incidentRepository.findRecent(1);
        assertFalse(incidents.isEmpty(), "There should be at least one incident from seeding or prior tests");

        Incident inc = incidents.get(0);
        assertNotNull(inc.getIncidentNumber(), "Incident number must be populated");
        assertNotNull(inc.getSeverity(), "Severity must be populated");
        assertNotNull(inc.getConfidenceScore(), "Confidence score must be populated");
        assertNotNull(inc.getEvidenceJson(), "Evidence JSON payload must be present");
        assertTrue(inc.getEvidenceJson().contains("{") && inc.getEvidenceJson().contains("}"),
                "Evidence JSON must be valid JSON format");
    }

    // =========================================================================
    // TEST 10: Incident status update with reviewer tracking
    // =========================================================================
    @Test
    @DisplayName("Test 10: Incident status transition records reviewer username and timestamp")
    void test10_incidentStatusUpdateWithReviewerTracking() {
        List<Incident> list = incidentRepository.findRecent(1);
        assertFalse(list.isEmpty());
        Incident inc = list.get(0);

        String notes = "Verified via SIEM logs and validated with academic registrar. Remediation closed.";
        boolean updated = incidentService.updateIncidentStatus(
                inc.getId(), "RESOLVED", notes, "admin01", admin01.getId(), "127.0.0.1"
        );

        assertTrue(updated, "Status update must succeed");

        Incident refreshed = incidentRepository.findById(inc.getId()).orElseThrow();
        assertEquals("RESOLVED", refreshed.getStatus(), "Status must be updated to RESOLVED");
        assertEquals("admin01", refreshed.getReviewedBy(), "Reviewer username must be recorded as admin01");
        assertNotNull(refreshed.getReviewedAt(), "Review timestamp must be recorded");
        assertTrue(refreshed.getInvestigationNotes().contains("SIEM logs"),
                "Investigation notes must be persisted");
    }

    // =========================================================================
    // BONUS INTEGRATION TESTS: Advanced Phase 3 Intelligence Modules
    // =========================================================================
    @Test
    @DisplayName("Bonus: Sentinel Insight Engine generates 8-point explainable intelligence")
    void testSentinelInsightGeneration() {
        List<Incident> incidents = incidentRepository.findRecent(1);
        assertFalse(incidents.isEmpty());
        Incident inc = incidents.get(0);

        ExplainableDossier dossier = sentinelInsightService.generateDossier(inc.getId()).orElseThrow();
        assertNotNull(dossier);
        assertEquals(inc.getIncidentNumber(), dossier.getIncidentNumber());
        assertNotNull(dossier.getWhatHappened());
        assertNotNull(dossier.getWhoPerformedAction());
        assertNotNull(dossier.getWhyFlagged());
        assertNotNull(dossier.getPotentialImpact());
        assertNotNull(dossier.getRecommendedNextSteps());
    }

    @Test
    @DisplayName("Bonus: Attack Simulation Lab executes real scenario and stores history")
    void testAttackSimulationExecution() {
        SimulationResponse res = simulationService.runSimulation("UNAUTHORIZED_ATTENDANCE", "student101", "127.0.0.1");
        assertNotNull(res);
        assertTrue(res.isDetected());
        assertNotNull(res.getIncidentId());
        assertEquals("RULE-ATT-001", res.getRuleCode());
    }

    @Test
    @DisplayName("Bonus: Security Health Indicator computes score via transparent formula")
    void testSecurityHealthScoreCalculation() {
        Map<String, Object> metrics = analyticsService.getDashboardMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("healthScore"));
        assertTrue(metrics.containsKey("healthStatus"));
        assertTrue(metrics.containsKey("securityHealth"));

        int score = (int) metrics.get("healthScore");
        assertTrue(score >= 0 && score <= 100, "Health score must be between 0 and 100");
    }

    @Test
    @DisplayName("Bonus: What-If Policy Testing evaluates isolated threshold delta")
    void testWhatIfPolicyTestingSandbox() {
        Map<String, Object> testResult = policyTestingService.runWhatIfPolicyTest("RULE-MOD-002", 5, 60, "HIGH", "SAMPLE_FACULTY_BURST");
        assertNotNull(testResult);
        assertEquals("RULE-MOD-002", testResult.get("ruleCode"));
        assertTrue(testResult.containsKey("baselineDetected"));
        assertTrue(testResult.containsKey("testDetected"));
    }

    @Test
    @DisplayName("Bonus: Report Generation exports CSV and PDF successfully")
    void testReportGenerationExports() {
        byte[] csvBytes = reportExportService.exportIncidentsCsv();
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0, "CSV bytes must not be empty");

        byte[] pdfBytes = reportExportService.exportIncidentsPdf();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "PDF bytes must not be empty");
        // Check PDF header signature: %PDF-
        assertTrue(new String(pdfBytes, 0, Math.min(pdfBytes.length, 5)).startsWith("%PDF"),
                "Generated file must have valid PDF header");
    }
}
