package com.netsentinel.web.engine.rules;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.DetectionResult;
import com.netsentinel.web.engine.SentinelInsightGenerator;
import com.netsentinel.web.model.DetectionRuleConfig;
import com.netsentinel.web.repository.AcademicRepository;
import com.netsentinel.web.repository.SubjectRepository;
import org.springframework.stereotype.Component;

/**
 * RULE 1 — Unauthorized Attendance Modification (RULE-ATT-001)
 * 
 * Category: AUTHORIZATION
 * Severity: HIGH
 * 
 * Detection Logic:
 * Checks:
 * 1. Authenticated faculty identity (actorId, actorRole)
 * 2. Faculty-subject assignment: Is faculty assigned to teach the course?
 * 3. Student enrollment: Is the target student enrolled in the course?
 * 4. Role breach: If actor is a STUDENT attempting attendance modification.
 * 
 * Demonstrates:
 * - Inheritance from BaseDetectionRule
 * - Overriding abstract evaluateRule()
 * - String formatting and forensic evidence JSON generation
 */
@Component
public class UnauthorizedAttendanceRule extends BaseDetectionRule {

    private final SubjectRepository subjectRepository;
    private final AcademicRepository academicRepository;

    public UnauthorizedAttendanceRule(SubjectRepository subjectRepository, 
                                      AcademicRepository academicRepository, 
                                      SentinelInsightGenerator insightGenerator) {
        super("RULE-ATT-001", "Unauthorized Attendance Modification", "AUTHORIZATION", "HIGH", insightGenerator);
        this.subjectRepository = subjectRepository;
        this.academicRepository = academicRepository;
    }

    @Override
    protected DetectionResult evaluateRule(DetectionContext ctx, DetectionRuleConfig config) {
        // Trigger condition 1: Student role attempting attendance mutation
        boolean isStudentAttempt = "STUDENT".equalsIgnoreCase(ctx.getActorRole()) && 
                (ctx.getAction().contains("ATTENDANCE") || ctx.getTargetResource().contains("attendance"));

        // Trigger condition 2: Faculty attempting attendance on a subject they are not assigned to
        boolean isUnassignedFaculty = false;
        Long subId = ctx.getSubjectId();
        if (subId == null && ctx.getAttribute("subject_id") != null) {
            Object s = ctx.getAttribute("subject_id");
            if (s instanceof Number) subId = ((Number) s).longValue();
        }
        
        if (Boolean.FALSE.equals(ctx.getAttribute("is_assigned")) || "ATTENDANCE_RECORD_UNAUTHORIZED".equalsIgnoreCase(ctx.getAction())) {
            isUnassignedFaculty = true;
        } else if ("FACULTY".equalsIgnoreCase(ctx.getActorRole()) && subId != null && ctx.getActorId() != null) {
            boolean assigned = subjectRepository.isFacultyAssigned(ctx.getActorId(), subId);
            if (!assigned) {
                isUnassignedFaculty = true;
            }
        }

        // Trigger condition 3: Student not enrolled in the target subject
        boolean isUnenrolledStudent = false;
        if (Boolean.FALSE.equals(ctx.getAttribute("is_enrolled"))) {
            isUnenrolledStudent = true;
        } else if (subId != null && ctx.getStudentId() != null) {
            boolean enrolled = academicRepository.isStudentEnrolledInSubject(ctx.getStudentId(), subId);
            if (!enrolled && ("FACULTY".equalsIgnoreCase(ctx.getActorRole()) || "STUDENT".equalsIgnoreCase(ctx.getActorRole()))) {
                isUnenrolledStudent = true;
            }
        }

        // Trigger condition 4: Explicit threat simulation flag
        boolean isSimulation = Boolean.TRUE.equals(ctx.getAttribute("simulation_unauthorized_attendance"));

        if (isStudentAttempt || isUnassignedFaculty || isUnenrolledStudent || isSimulation) {
            String title = "Unauthorized Attendance Alteration Intercepted";
            String severity = resolveSeverity(config);
            int confidence = 96;

            String subjectTag = ctx.getSubjectCode() != null ? ctx.getSubjectCode() : 
                                (ctx.getSubjectId() != null ? "ID:" + ctx.getSubjectId() : "CS301");
            String targetStudentTag = ctx.getStudentId() != null ? "Student ID: " + ctx.getStudentId() : "Enrolled Roster";

            String violationType;
            if (isStudentAttempt) {
                violationType = "STUDENT_ROLE_BREACH";
            } else if (isUnassignedFaculty) {
                violationType = "UNASSIGNED_FACULTY_OVERREACH";
            } else if (isUnenrolledStudent) {
                violationType = "UNENROLLED_STUDENT_TARGET";
            } else {
                violationType = "SIMULATED_ATTENDANCE_TAMPER";
            }

            String reason = String.format("Actor '%s' (%s) attempted unauthorized attendance alteration on subject '%s' for '%s'. Violation type: %s.",
                    ctx.getActorUsername(), ctx.getActorRole(), subjectTag, targetStudentTag, violationType);

            String evidenceJson = String.format("""
                {"actor_username": "%s", "actor_role": "%s", "target_subject": "%s", "target_student": "%s", "target_resource": "%s", "ip_address": "%s", "violation_type": "%s"}
                """.trim(),
                    ctx.getActorUsername(), ctx.getActorRole(), subjectTag, targetStudentTag, ctx.getTargetResource(), ctx.getIpAddress(), violationType
            );

            String contextDetails = String.format(
                "- Target Subject: %s\n- Target Student: %s\n- Origin IP: %s\n- Enforcement: Direct interception by NetSentinel Kernel (Action Blocked).",
                subjectTag, targetStudentTag, ctx.getIpAddress()
            );

            String insight = insightGenerator.generateInsight(getRuleCode(), ctx.getActorUsername(), ctx.getActorRole(), ctx.getTargetResource(), confidence, contextDetails);

            String recommendedAction = String.format(
                "1. Cross-reference academic department schedule for faculty assignment.\n" +
                "2. Check IP %s audit records for prior attendance manipulation attempts.\n" +
                "3. Verify physical attendance sheet signatures before any manual adjustment.\n" +
                "4. Dispatch warning alert to the department registrar.",
                ctx.getIpAddress()
            );

            return DetectionResult.detected(getRuleCode(), title, getCategory(), severity, confidence, reason, evidenceJson, insight, recommendedAction);
        }

        return DetectionResult.safe();
    }
}
