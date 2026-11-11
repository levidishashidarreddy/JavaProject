package com.netsentinel.web.service;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.DetectionResult;
import com.netsentinel.web.engine.rules.DetectionRule;
import com.netsentinel.web.model.DetectionRuleConfig;
import com.netsentinel.web.repository.DetectionRuleRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service providing What-If Policy Simulation and Calibration.
 * 
 * Allows security administrators to test threshold and time window changes
 * against synthetic activity patterns in an isolated sandbox without polluting
 * production/demo database records.
 */
@Service
public class PolicyTestingService {

    private final DetectionRuleRepository ruleRepository;
    private final List<DetectionRule> detectionRules;

    public PolicyTestingService(DetectionRuleRepository ruleRepository, List<DetectionRule> detectionRules) {
        this.ruleRepository = ruleRepository;
        this.detectionRules = detectionRules;
    }

    public Map<String, Object> runWhatIfPolicyTest(String ruleCode, int testThreshold, int testWindowSeconds, 
                                                   String testSeverity, String activityPattern) {
        Optional<DetectionRuleConfig> currentConfigOpt = ruleRepository.findByCode(ruleCode);
        DetectionRuleConfig currentConfig = currentConfigOpt.orElseGet(() -> {
            DetectionRuleConfig cfg = new DetectionRuleConfig();
            cfg.setRuleCode(ruleCode);
            cfg.setEnabled(true);
            cfg.setThreshold(3);
            cfg.setTimeWindowSeconds(60);
            cfg.setSeverity("CRITICAL");
            return cfg;
        });

        // Find matching detection rule bean
        DetectionRule rule = detectionRules.stream()
                .filter(r -> r.getRuleCode().equalsIgnoreCase(ruleCode))
                .findFirst()
                .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ruleCode", ruleCode);
        result.put("ruleName", currentConfig.getRuleName() != null ? currentConfig.getRuleName() : ruleCode);
        result.put("activityPattern", activityPattern);
        result.put("isolatedFromProduction", true);

        // Baseline configuration
        int baseThreshold = currentConfig.getThreshold();
        int baseWindow = currentConfig.getTimeWindowSeconds();
        String baseSeverity = currentConfig.getSeverity();

        result.put("currentThreshold", baseThreshold);
        result.put("currentWindowSeconds", baseWindow);
        result.put("currentSeverity", baseSeverity);

        // Simulated proposed configuration
        result.put("simulatedThreshold", testThreshold);
        result.put("simulatedWindowSeconds", testWindowSeconds);
        result.put("simulatedSeverity", testSeverity);

        // Generate synthetic context based on activityPattern
        DetectionContext testContext = buildTestContext(ruleCode, activityPattern);

        // 1. Evaluate under current configuration
        DetectionResult currentEval = (rule != null) ? rule.evaluate(testContext, currentConfig) : DetectionResult.safe();
        boolean currentTriggered = currentEval.isDetected();

        // 2. Evaluate under simulated configuration
        DetectionRuleConfig proposedConfig = new DetectionRuleConfig();
        proposedConfig.setRuleCode(ruleCode);
        proposedConfig.setEnabled(true);
        proposedConfig.setThreshold(testThreshold);
        proposedConfig.setTimeWindowSeconds(testWindowSeconds);
        proposedConfig.setSeverity(testSeverity);

        DetectionResult simulatedEval = (rule != null) ? rule.evaluate(testContext, proposedConfig) : DetectionResult.safe();
        boolean simulatedTriggered = simulatedEval.isDetected();

        result.put("currentTriggered", currentTriggered);
        result.put("baselineDetected", currentTriggered);
        result.put("currentOutcome", currentTriggered ? "TRIGGERED (" + currentEval.getSeverity() + ")" : "SAFE (Action Allowed)");
        result.put("simulatedTriggered", simulatedTriggered);
        result.put("testDetected", simulatedTriggered);
        result.put("simulatedOutcome", simulatedTriggered ? "TRIGGERED (" + testSeverity + ")" : "SAFE (Action Allowed)");

        // 3. Before vs After Delta Analysis
        String delta;
        if (!currentTriggered && simulatedTriggered) {
            delta = "SENSITIVITY INCREASED: The proposed policy flags this activity pattern as suspicious. Prevents stealthy low-volume anomalies.";
        } else if (currentTriggered && !simulatedTriggered) {
            delta = "TOLERANCE INCREASED: The proposed policy permits this activity without raising an incident. Mitigates potential false alarms.";
        } else if (currentTriggered) {
            delta = "UNCHANGED (FLAGGED): Both current and proposed policies intercept this pattern.";
        } else {
            delta = "UNCHANGED (SAFE): Both current and proposed policies classify this pattern as benign.";
        }
        result.put("decisionDelta", delta);
        result.put("analysisSummary", String.format(
                "Policy Test for %s with pattern '%s': Baseline [%d events/%ds] -> %s | Proposed [%d events/%ds] -> %s.",
                ruleCode, activityPattern, baseThreshold, baseWindow, (currentTriggered ? "DETECTED" : "PASSED"),
                testThreshold, testWindowSeconds, (simulatedTriggered ? "DETECTED" : "PASSED")));

        return result;
    }

    private DetectionContext buildTestContext(String ruleCode, String pattern) {
        DetectionContext ctx = new DetectionContext();
        ctx.setIpAddress("192.168.1.199");
        ctx.setUserAgent("Mozilla/5.0 What-If Test Agent");

        switch (ruleCode) {
            case "RULE-ATT-001" -> {
                if ("STUDENT_ATTENDANCE_ATTEMPT".equalsIgnoreCase(pattern)) {
                    ctx.setActorId(1L);
                    ctx.setActorUsername("student101");
                    ctx.setActorRole("STUDENT");
                    ctx.setAction("ATTENDANCE_RECORD");
                    ctx.setTargetResource("/faculty/attendance/record");
                    ctx.setSubjectId(1L);
                    ctx.addAttribute("simulation_unauthorized_attendance", true);
                } else {
                    // Normal faculty assigned update
                    ctx.setActorId(2L);
                    ctx.setActorUsername("faculty101");
                    ctx.setActorRole("FACULTY");
                    ctx.setAction("ATTENDANCE_RECORD");
                    ctx.setTargetResource("/faculty/attendance/record");
                    ctx.setSubjectId(1L);
                }
            }
            case "RULE-MOD-002" -> {
                ctx.setActorId(3L);
                ctx.setActorUsername("faculty102");
                ctx.setActorRole("FACULTY");
                ctx.setAction("MARKS_RECORD");
                ctx.setTargetResource("/faculty/marks/record");
                if ("BURST_BATCH_GRADING".equalsIgnoreCase(pattern)) {
                    ctx.addAttribute("simulation_bulk_modification", true);
                }
            }
            case "RULE-PRV-003" -> {
                if ("UNAUTHORIZED_ROLE_CHANGE".equalsIgnoreCase(pattern)) {
                    ctx.setActorId(1L);
                    ctx.setActorUsername("student101");
                    ctx.setActorRole("STUDENT");
                    ctx.setAction("USER_ROLE_CHANGE");
                    ctx.setTargetResource("/admin/users/1/role");
                    ctx.addAttribute("simulation_privilege_escalation", true);
                } else {
                    ctx.setActorId(4L);
                    ctx.setActorUsername("admin01");
                    ctx.setActorRole("ADMIN");
                    ctx.setAction("USER_ROLE_CHANGE");
                    ctx.setTargetResource("/admin/users/1/role");
                }
            }
            case "RULE-REC-004" -> {
                if ("HORIZONTAL_STUDENT_PROBE".equalsIgnoreCase(pattern)) {
                    ctx.setActorId(1L);
                    ctx.setActorUsername("student101");
                    ctx.setActorRole("STUDENT");
                    ctx.setAction("UNAUTHORIZED_STUDENT_RECORD_ACCESS");
                    ctx.setTargetResource("/student/marks?studentId=5");
                    ctx.setStudentId(5L);
                    ctx.addAttribute("simulation_unauthorized_student_access", true);
                } else {
                    ctx.setActorId(1L);
                    ctx.setActorUsername("student101");
                    ctx.setActorRole("STUDENT");
                    ctx.setAction("VIEW_MY_MARKS");
                    ctx.setTargetResource("/student/marks");
                    ctx.setStudentId(1L);
                }
            }
            default -> {
                ctx.setActorId(1L);
                ctx.setActorUsername("student101");
                ctx.setActorRole("STUDENT");
                ctx.setAction("VIEW_PORTAL");
                ctx.setTargetResource("/");
            }
        }

        return ctx;
    }
}

