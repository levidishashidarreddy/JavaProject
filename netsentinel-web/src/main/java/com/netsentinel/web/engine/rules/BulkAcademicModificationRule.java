package com.netsentinel.web.engine.rules;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.DetectionResult;
import com.netsentinel.web.engine.SentinelInsightGenerator;
import com.netsentinel.web.model.DetectionRuleConfig;
import com.netsentinel.web.model.Incident;
import com.netsentinel.web.repository.ActivityLogRepository;
import com.netsentinel.web.repository.IncidentRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RULE 2 — Bulk Academic Record Modification (RULE-MOD-002)
 * 
 * Category: INTEGRITY
 * Severity: CRITICAL (Configurable: default 3 modifications / 60 seconds)
 * 
 * Demonstrates:
 * - Data structures: LinkedList, HashMap (ConcurrentHashMap), Iterator
 * - Sliding time window algorithm with Iterator-based eviction
 * - Sensible incident deduplication: avoids duplicate incident flooding for continuous bursts
 * - Inheritance from BaseDetectionRule
 */
@Component
public class BulkAcademicModificationRule extends BaseDetectionRule {

    private final ActivityLogRepository activityLogRepository;
    private final IncidentRepository incidentRepository;

    /**
     * In-memory rolling window tracker using Map and LinkedList.
     * Demonstrates:
     * - Java Collection Framework: Map<String, LinkedList<Long>>
     * - Sliding window timestamp tracking per actor
     */
    private final Map<String, LinkedList<Long>> actorTimestampsMap = new ConcurrentHashMap<>();

    public BulkAcademicModificationRule(ActivityLogRepository activityLogRepository, 
                                        IncidentRepository incidentRepository,
                                        SentinelInsightGenerator insightGenerator) {
        super("RULE-MOD-002", "Bulk Academic Record Modification", "INTEGRITY", "CRITICAL", insightGenerator);
        this.activityLogRepository = activityLogRepository;
        this.incidentRepository = incidentRepository;
    }

    @Override
    protected DetectionResult evaluateRule(DetectionContext ctx, DetectionRuleConfig config) {
        // Configurable threshold & rolling time window (default 3 modifications in 60 seconds)
        int threshold = (config != null && config.getThreshold() != null) ? config.getThreshold() : 3;
        int windowSeconds = (config != null && config.getTimeWindowSeconds() != null) ? config.getTimeWindowSeconds() : 60;

        boolean isAcademicMutation = "MARKS_RECORD".equalsIgnoreCase(ctx.getAction()) || 
                                     "BULK_MARKS_UPDATE".equalsIgnoreCase(ctx.getAction()) ||
                                     "ATTENDANCE_RECORD".equalsIgnoreCase(ctx.getAction()) ||
                                     ctx.getTargetResource().contains("/marks") ||
                                     ctx.getTargetResource().contains("/attendance");

        boolean isSimulation = Boolean.TRUE.equals(ctx.getAttribute("simulation_bulk_modification"));

        // Explicit access boundary violations belong to specific rules (RULE-ATT-001, RULE-PRV-003, RULE-REC-004)
        if ("ATTENDANCE_RECORD_UNAUTHORIZED".equalsIgnoreCase(ctx.getAction()) || 
            "UNAUTHORIZED_STUDENT_RECORD_ACCESS".equalsIgnoreCase(ctx.getAction()) ||
            "USER_ROLE_CHANGE_UNAUTHORIZED".equalsIgnoreCase(ctx.getAction())) {
            return DetectionResult.safe();
        }

        if (!isAcademicMutation && !isSimulation) {
            return DetectionResult.safe();
        }

        String username = ctx.getActorUsername() != null ? ctx.getActorUsername().toLowerCase() : "anonymous";
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        // Update in-memory sliding window using LinkedList and Iterator
        LinkedList<Long> timestamps = actorTimestampsMap.computeIfAbsent(username, k -> new LinkedList<>());
        synchronized (timestamps) {
            // Syllabus demonstration: Using Iterator to prune expired timestamps
            Iterator<Long> it = timestamps.iterator();
            while (it.hasNext()) {
                Long ts = it.next();
                if (ts < windowStart) {
                    it.remove();
                } else {
                    break; // LinkedList is chronologically ordered
                }
            }
            // Add current modification timestamp
            timestamps.add(now);
        }

        int memoryCount = timestamps.size();
        // Also query database for cross-restart/multi-instance consistency
        int dbCount = activityLogRepository.countRecentAcademicMutationsInWindow(username, windowSeconds);
        int totalRecentModifications = Math.max(memoryCount, dbCount);

        if (totalRecentModifications >= threshold || isSimulation) {
            // Deduplication Check: avoid generating unlimited duplicate incidents for the same continuous burst
            if (!isSimulation) {
                Optional<Incident> activeIncident = incidentRepository.findActiveIncidentByRuleAndActor(getRuleCode(), username, windowSeconds);
                if (activeIncident.isPresent()) {
                    // An open incident already exists for this burst; return safe to avoid flooding
                    return DetectionResult.safe();
                }
            }

            int displayCount = isSimulation ? Math.max(threshold + 2, 7) : totalRecentModifications;
            String title = "Bulk Academic Record Modification Burst";
            String severity = resolveSeverity(config);
            int confidence = 92;

            String reason = String.format("Actor '%s' initiated %d grade/attendance adjustments within %d seconds, exceeding the velocity threshold of %d.",
                    ctx.getActorUsername(), displayCount, windowSeconds, threshold);

            String evidenceJson = String.format("""
                {"burst_count": %d, "time_window_seconds": %d, "threshold": %d, "actor": "%s", "ip_address": "%s", "velocity_anomaly": "HIGH_FREQUENCY_BATCH"}
                """.trim(),
                    displayCount, windowSeconds, threshold, ctx.getActorUsername(), ctx.getIpAddress()
            );

            String contextDetails = String.format(
                "- Rapid Modification Count: %d operations in %d seconds\n- Configured Velocity Limit: %d\n- Potential Risk: Mass grading tampering or compromised instructor session.",
                displayCount, windowSeconds, threshold
            );

            String insight = insightGenerator.generateInsight(getRuleCode(), ctx.getActorUsername(), ctx.getActorRole(), ctx.getTargetResource(), confidence, contextDetails);

            String recommendedAction = """
                1. Review academic modification audit logs for aberrant grade distributions.
                2. Contact the instructor to verify whether this was an authorized batch grading session.
                3. If unexpected, temporarily lock grading permissions and inspect browser session tokens.
                4. Verify database transactions to ensure rollbacks are available if needed.
                """.trim();

            return DetectionResult.detected(getRuleCode(), title, getCategory(), severity, confidence, reason, evidenceJson, insight, recommendedAction);
        }

        return DetectionResult.safe();
    }
}
