package com.netsentinel.web.engine.rules;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.DetectionResult;
import com.netsentinel.web.engine.SentinelInsightGenerator;
import com.netsentinel.web.model.DetectionRuleConfig;
import com.netsentinel.web.repository.AcademicRepository;
import org.springframework.stereotype.Component;

/**
 * RULE 4 — Unauthorized Student Record Access (RULE-REC-004)
 * 
 * Category: ACCESS_CONTROL
 * Severity: HIGH
 * 
 * Detection Logic:
 * Checks:
 * 1. Authenticated user ID (actorId, actorRole)
 * 2. Requested student record owner ID (requestedStudentId)
 * 3. Scope validation:
 *    - STUDENT: Can only access own records (actorId == requestedStudentId).
 *    - FACULTY: Can only access records of students enrolled in subjects taught by this faculty.
 *    - ADMIN: Has administrative visibility.
 * 
 * Demonstrates:
 * - Multi-tier role-based access validation in Java
 * - Inheritance from BaseDetectionRule
 * - Prevention of IDOR (Insecure Direct Object Reference) and horizontal privilege escalation
 */
@Component
public class UnauthorizedStudentAccessRule extends BaseDetectionRule {

    private final AcademicRepository academicRepository;

    public UnauthorizedStudentAccessRule(AcademicRepository academicRepository, 
                                         SentinelInsightGenerator insightGenerator) {
        super("RULE-REC-004", "Unauthorized Student Record Access", "ACCESS_CONTROL", "HIGH", insightGenerator);
        this.academicRepository = academicRepository;
    }

    @Override
    protected DetectionResult evaluateRule(DetectionContext ctx, DetectionRuleConfig config) {
        Long actorId = ctx.getActorId();
        String role = ctx.getActorRole() != null ? ctx.getActorRole().toUpperCase() : "ANONYMOUS";
        Long requestedStudentId = ctx.getStudentId();

        boolean isSimulation = Boolean.TRUE.equals(ctx.getAttribute("simulation_record_scraping")) ||
                               Boolean.TRUE.equals(ctx.getAttribute("simulation_unauthorized_student_access"));

        boolean isUnauthorizedStudentAttempt = false;
        boolean isUnauthorizedFacultyAttempt = false;

        // Check 1: Student accessing another student's records (Horizontal IDOR)
        if ("STUDENT".equals(role) && requestedStudentId != null && actorId != null) {
            if (!actorId.equals(requestedStudentId)) {
                isUnauthorizedStudentAttempt = true;
            }
        }

        // Check 2: Faculty accessing student not in any of their assigned courses
        if ("FACULTY".equals(role) && requestedStudentId != null && actorId != null) {
            boolean assigned = academicRepository.isStudentAssignedToFaculty(requestedStudentId, actorId);
            if (!assigned) {
                isUnauthorizedFacultyAttempt = true;
            }
        }

        // Check 3: Explicit action flag from service
        boolean isFlaggedAction = "UNAUTHORIZED_STUDENT_RECORD_ACCESS".equalsIgnoreCase(ctx.getAction());

        if (isUnauthorizedStudentAttempt || isUnauthorizedFacultyAttempt || isFlaggedAction || isSimulation) {
            String title = "Unauthorized Student Record Access Intercepted";
            String severity = resolveSeverity(config);
            int confidence = 95;

            String breachType;
            if (isUnauthorizedStudentAttempt) {
                breachType = "STUDENT_HORIZONTAL_IDOR";
            } else if (isUnauthorizedFacultyAttempt) {
                breachType = "FACULTY_OUT_OF_SCOPE_QUERY";
            } else {
                breachType = "UNAUTHORIZED_RECORD_PROBE";
            }

            String targetTag = requestedStudentId != null ? "Student ID: " + requestedStudentId : ctx.getTargetResource();

            String reason = String.format("Actor '%s' (%s) attempted unauthorized access to student academic records for '%s'. Breach classification: %s.",
                    ctx.getActorUsername(), role, targetTag, breachType);

            String evidenceJson = String.format("""
                {"actor_id": %s, "actor_username": "%s", "actor_role": "%s", "target_student_id": %s, "target_resource": "%s", "breach_type": "%s", "ip_address": "%s"}
                """.trim(),
                    actorId != null ? actorId : "null",
                    ctx.getActorUsername(),
                    role,
                    requestedStudentId != null ? requestedStudentId : "null",
                    ctx.getTargetResource(),
                    breachType,
                    ctx.getIpAddress()
            );

            String contextDetails = String.format(
                "- Requester: %s (Role: %s)\n- Target Record Owner: %s\n- Scope Violation: Requester is not authorized to inspect target student transcript/grades.\n- Enforcement: Action blocked by NetSentinel Security Kernel.",
                ctx.getActorUsername(), role, targetTag
            );

            String insight = insightGenerator.generateInsight(getRuleCode(), ctx.getActorUsername(), role, ctx.getTargetResource(), confidence, contextDetails);

            String recommendedAction = """
                1. Verify requester session tokens to detect parameter tampering (IDOR).
                2. Audit web server logs for sequential student identifier querying.
                3. Ensure gradebook and transcript APIs return 403 Forbidden without exposing student metadata.
                4. Inform academic integrity officer if horizontal probing was deliberate.
                """.trim();

            return DetectionResult.detected(getRuleCode(), title, getCategory(), severity, confidence, reason, evidenceJson, insight, recommendedAction);
        }

        return DetectionResult.safe();
    }
}

