package com.netsentinel.web.service;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.SecurityDetectionEngine;
import com.netsentinel.web.model.ActivityLog;
import com.netsentinel.web.model.Incident;
import com.netsentinel.web.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Reusable Activity Logging and Audit Service for NetSentinel.
 * 
 * Demonstrates:
 * - Method Overloading (multiple log variants with distinct parameter signatures)
 * - Defensive parameter normalization
 * - Correlation tracing with UUID request IDs
 * - Server-side audit generation ensuring tamper-resistance
 */
@Service
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository activityLogRepository;
    private final SecurityDetectionEngine detectionEngine;
    private final FirebaseDatabaseService firebaseDatabaseService;

    public ActivityLogService(ActivityLogRepository activityLogRepository,
                              SecurityDetectionEngine detectionEngine,
                              FirebaseDatabaseService firebaseDatabaseService) {
        this.activityLogRepository = activityLogRepository;
        this.detectionEngine = detectionEngine;
        this.firebaseDatabaseService = firebaseDatabaseService;
    }

    /**
     * Overloaded Variant 1: Log an ALLOWED action (persisted as SUCCESS to satisfy SQLite check constraints).
     */
    public void logAllowed(Long actorId, String username, String role, String action, 
                           String targetType, String targetId, String resource, String ip, String details) {
        log(actorId, username, role, action, targetType, targetId, resource, "SUCCESS", ip, "NetSentinel-Kernel", details, UUID.randomUUID().toString(), null);
    }

    /**
     * Overloaded Variant 2: Log a DENIED action and trigger behavioral detection inspection.
     */
    public Optional<Incident> logDenied(Long actorId, String username, String role, String action, 
                                        String targetType, String targetId, String resource, String ip, 
                                        String details, Map<String, Object> evidenceContext) {
        return log(actorId, username, role, action, targetType, targetId, resource, "DENIED", ip, "NetSentinel-Kernel", details, UUID.randomUUID().toString(), evidenceContext);
    }

    /**
     * Overloaded Variant 3: General logging with outcome and request ID.
     */
    public Optional<Incident> log(Long actorId, String username, String role, String action, 
                                  String targetType, String targetId, String resource, String outcome, 
                                  String ip, String userAgent, String details, String requestId, 
                                  Map<String, Object> extraContext) {
        String safeOutcome = (outcome == null || "ALLOWED".equalsIgnoreCase(outcome)) ? "SUCCESS" : outcome.toUpperCase();
        ActivityLog activityLog = new ActivityLog(
                actorId,
                username != null ? username : "anonymous",
                role != null ? role : "ANONYMOUS",
                action,
                targetType != null ? targetType : "RESOURCE",
                targetId,
                resource != null ? resource : "/",
                safeOutcome,
                ip != null ? ip : "127.0.0.1",
                userAgent != null ? userAgent : "Unknown",
                details,
                requestId != null ? requestId : UUID.randomUUID().toString()
        );

        activityLogRepository.log(activityLog);
        if (firebaseDatabaseService != null) {
            firebaseDatabaseService.syncActivityLog(activityLog);
        }

        // Security inspection dispatch
        DetectionContext ctx = new DetectionContext(actorId, username, role, action, resource, ip);
        ctx.setUserAgent(userAgent);
        ctx.setTargetType(targetType);
        ctx.setTargetId(targetId);
        ctx.setRequestId(requestId);
        if (extraContext != null) {
            ctx.setAttributes(extraContext);
            if (extraContext.containsKey("subject_id")) {
                Object v = extraContext.get("subject_id");
                if (v instanceof Number) ctx.setSubjectId(((Number) v).longValue());
                else if (v instanceof String) {
                    try { ctx.setSubjectId(Long.parseLong((String) v)); } catch (Exception ignored) {}
                }
            }
            if (extraContext.containsKey("target_student_id")) {
                Object v = extraContext.get("target_student_id");
                if (v instanceof Number) ctx.setStudentId(((Number) v).longValue());
                else if (v instanceof String) {
                    try { ctx.setStudentId(Long.parseLong((String) v)); } catch (Exception ignored) {}
                }
            }
            if (extraContext.containsKey("subject_code")) {
                ctx.setSubjectCode(String.valueOf(extraContext.get("subject_code")));
            }
        }
        if (ctx.getStudentId() == null && "STUDENT_RECORD".equalsIgnoreCase(targetType) && targetId != null) {
            try { ctx.setStudentId(Long.parseLong(targetId)); } catch (Exception ignored) {}
        }

        return detectionEngine.evaluate(ctx);
    }

    /**
     * Overloaded Variant 4: Legacy compatible log signature.
     */
    public void log(Long actorId, String username, String role, String action, String resource, String outcome, String ip, String userAgent, String details) {
        log(actorId, username, role, action, "RESOURCE", null, resource, outcome, ip, userAgent, details, UUID.randomUUID().toString(), null);
    }

    /**
     * Overloaded Variant 5: Legacy compatible logAndInspect signature.
     */
    public Optional<Incident> logAndInspect(Long actorId, String username, String role, String action, 
                                            String resource, String outcome, String ip, String userAgent, 
                                            String details, Map<String, Object> extraAttrs) {
        return log(actorId, username, role, action, "RESOURCE", null, resource, outcome, ip, userAgent, details, UUID.randomUUID().toString(), extraAttrs);
    }

    public List<ActivityLog> getRecentLogs(int limit) {
        return activityLogRepository.findRecent(limit);
    }

    public List<ActivityLog> getRecentLogsByActor(String username, int limit) {
        return activityLogRepository.findRecentByActor(username, limit);
    }

    public List<ActivityLog> searchLogs(String actor, String role, String action, String outcome, int page, int pageSize) {
        int offset = Math.max(0, (page - 1) * pageSize);
        return activityLogRepository.searchLogs(actor, role, action, outcome, pageSize, offset);
    }

    public int countSearchLogs(String actor, String role, String action, String outcome) {
        return activityLogRepository.countSearchLogs(actor, role, action, outcome);
    }

    public List<Map<String, Object>> getActivityCountsByHour() {
        return activityLogRepository.findActivityCountsByHour();
    }

    public List<Map<String, Object>> getActivityDistributionByAction() {
        return activityLogRepository.findActivityDistributionByAction();
    }

    public int countTotalLogs() {
        return activityLogRepository.countTotal();
    }
}

