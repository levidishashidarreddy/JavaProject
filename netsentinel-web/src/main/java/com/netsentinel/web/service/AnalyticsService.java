package com.netsentinel.web.service;

import com.netsentinel.web.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Security Analytics and Campus Health Service.
 * 
 * Provides:
 * - Transparent Security Health Indicator formula computation
 * - Aggregated dashboard metrics across incidents and activity logs
 * - Multi-criteria date filtering (TODAY, 7_DAYS, 30_DAYS, ALL)
 * - Visual chart breakdown data for SOC console
 */
@Service
public class AnalyticsService {

    private final IncidentRepository incidentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final DetectionRuleRepository ruleRepository;

    public AnalyticsService(IncidentRepository incidentRepository,
                            ActivityLogRepository activityLogRepository,
                            UserRepository userRepository,
                            SubjectRepository subjectRepository,
                            DetectionRuleRepository ruleRepository) {
        this.incidentRepository = incidentRepository;
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.ruleRepository = ruleRepository;
    }

    public Map<String, Object> getDashboardMetrics() {
        return getDashboardMetricsWithFilter("ALL");
    }

    public Map<String, Object> getDashboardMetricsWithFilter(String dateFilter) {
        Map<String, Object> metrics = new HashMap<>();

        Map<String, Object> incSummary = incidentRepository.getStatsSummary();
        int totalActivities = activityLogRepository.countTotal();
        int totalUsers = userRepository.countTotal();
        int totalSubjects = subjectRepository.countTotal();

        int totalIncidents = getSafeInt(incSummary, "total_incidents");
        int highCritical = getSafeInt(incSummary, "high_critical_count");
        int openIncidents = getSafeInt(incSummary, "open_count");
        int investigating = getSafeInt(incSummary, "investigating_count");
        int resolved = getSafeInt(incSummary, "resolved_count");

        // 1. Transparent Security Health Indicator Calculation
        Map<String, Object> healthData = calculateSecurityHealth(incSummary);

        metrics.put("totalActivities", totalActivities);
        metrics.put("totalIncidents", totalIncidents);
        metrics.put("highCriticalIncidents", highCritical);
        metrics.put("openIncidents", openIncidents);
        metrics.put("investigatingIncidents", investigating);
        metrics.put("resolvedIncidents", resolved);
        metrics.put("totalUsers", totalUsers);
        metrics.put("totalSubjects", totalSubjects);

        metrics.put("securityHealth", healthData);
        metrics.put("healthScore", healthData.get("score"));
        metrics.put("healthStatus", healthData.get("status"));
        metrics.put("threatIndex", 100 - (int) healthData.get("score"));

        metrics.put("severityDistribution", incidentRepository.getSeverityDistribution());
        metrics.put("categoryDistribution", incidentRepository.getCategoryDistribution());
        metrics.put("statusDistribution", incidentRepository.getIncidentsByStatusDistribution());
        metrics.put("topTriggeredRules", incidentRepository.getTopTriggeredRules(5));
        metrics.put("userWiseSummary", activityLogRepository.getUserWiseActivitySummary(6));

        metrics.put("activityHourly", activityLogRepository.findActivityCountsByHour());
        metrics.put("activityDistribution", activityLogRepository.findActivityDistributionByAction());
        metrics.put("activeRulesCount", ruleRepository.findAll().stream().filter(r -> r.isEnabled()).count());
        metrics.put("dateFilter", dateFilter != null ? dateFilter : "ALL");

        return metrics;
    }

    /**
     * Transparent Security Health Indicator Formula:
     * - Base Score: 100 points
     * - Deductions:
     *   - Open High/Critical: -12 points each (capped at -40)
     *   - Open Medium/Low: -6 points each (capped at -18)
     *   - Under Investigation: -4 points each (capped at -16)
     * - Bonuses:
     *   - Resolved Incidents: +3 points each (capped at +15 recovery)
     * - Clamped between [0, 100]
     */
    public Map<String, Object> calculateSecurityHealth(Map<String, Object> incSummary) {
        int total = getSafeInt(incSummary, "total_incidents");
        int highCrit = getSafeInt(incSummary, "high_critical_count");
        int open = getSafeInt(incSummary, "open_count");
        int investigating = getSafeInt(incSummary, "investigating_count");
        int resolved = getSafeInt(incSummary, "resolved_count");

        int openNormal = Math.max(0, open - highCrit);

        int baseScore = 100;
        int highCritDeduction = Math.min(40, highCrit * 12);
        int openNormalDeduction = Math.min(18, openNormal * 6);
        int investigatingDeduction = Math.min(16, investigating * 4);
        int resolvedBonus = Math.min(15, resolved * 3);

        int rawScore = baseScore - highCritDeduction - openNormalDeduction - investigatingDeduction + resolvedBonus;
        int finalScore = Math.max(5, Math.min(100, rawScore));

        String status;
        String color;
        String description;

        if (finalScore >= 85) {
            status = "SECURE";
            color = "#20A99A";
            description = "Campus security posture is optimal. Low threat volume with active incident response.";
        } else if (finalScore >= 70) {
            status = "ELEVATED RISK";
            color = "#E6A23C";
            description = "Active anomalous patterns flagged. Immediate review of open investigations advised.";
        } else {
            status = "CRITICAL ATTENTION";
            color = "#E05252";
            description = "High-severity policy violations or multi-course breaches detected. Urgent SOC action required.";
        }

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("score", finalScore);
        health.put("status", status);
        health.put("color", color);
        health.put("description", description);
        health.put("formulaExplanation", "Base 100 - (12 × Open High/Crit) - (6 × Open Med/Low) - (4 × Investigating) + (3 × Resolved). Capped [0-100].");
        health.put("breakdown", Map.of(
                "baseScore", baseScore,
                "highCritDeduction", highCritDeduction,
                "openNormalDeduction", openNormalDeduction,
                "investigatingDeduction", investigatingDeduction,
                "resolvedBonus", resolvedBonus
        ));

        return health;
    }

    private int getSafeInt(Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (Exception ignored) {}
        }
        return 0;
    }
}
