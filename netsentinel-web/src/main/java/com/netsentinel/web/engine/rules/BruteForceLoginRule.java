package com.netsentinel.web.engine.rules;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.DetectionResult;
import com.netsentinel.web.engine.SentinelInsightGenerator;
import com.netsentinel.web.model.DetectionRuleConfig;
import com.netsentinel.web.repository.ActivityLogRepository;
import org.springframework.stereotype.Component;

/**
 * RULE 6 — Credential Stuffing & Login Surge (RULE-BRT-006)
 * 
 * Category: AUTHENTICATION
 * Severity: HIGH
 */
@Component
public class BruteForceLoginRule extends BaseDetectionRule {

    private final ActivityLogRepository activityLogRepository;

    public BruteForceLoginRule(ActivityLogRepository activityLogRepository, SentinelInsightGenerator insightGenerator) {
        super("RULE-BRT-006", "Credential Stuffing & Login Surge", "AUTHENTICATION", "HIGH", insightGenerator);
        this.activityLogRepository = activityLogRepository;
    }

    @Override
    protected DetectionResult evaluateRule(DetectionContext ctx, DetectionRuleConfig config) {
        int threshold = (config != null && config.getThreshold() != null) ? config.getThreshold() : 3;
        int window = (config != null && config.getTimeWindowSeconds() != null) ? config.getTimeWindowSeconds() : 60;

        boolean isLoginFailure = "LOGIN_FAILED".equalsIgnoreCase(ctx.getAction());
        boolean isSimulation = Boolean.TRUE.equals(ctx.getAttribute("simulation_brute_force"));

        int failedCount = 0;
        if (isLoginFailure) {
            failedCount = activityLogRepository.countRecentActionsInWindow(ctx.getActorUsername(), "LOGIN_FAILED", window);
        }

        if (failedCount >= threshold || isSimulation) {
            int displayCount = isSimulation ? 5 : (failedCount + 1);
            String title = "Credential Abuse / Brute Force Surge Intercepted";
            String severity = resolveSeverity(config);
            int confidence = 95;

            String reason = String.format("Origin IP '%s' produced %d failed login attempts targeting '%s' in %d seconds.",
                    ctx.getIpAddress(), displayCount, ctx.getActorUsername(), window);

            String evidenceJson = String.format("""
                {"failed_attempts": %d, "window_seconds": %d, "target_user": "%s", "ip_address": "%s", "vector": "HTTP_POST_CREDENTIAL_STUFFING"}
                """.trim(),
                    displayCount, window, ctx.getActorUsername(), ctx.getIpAddress()
            );

            String contextDetails = String.format(
                "- Surge Velocity: %d failures in %d seconds\n- Target Account: %s\n- Mitigation: Temporary IP tarpit and rate limiting engaged.",
                displayCount, window, ctx.getActorUsername()
            );

            String insight = insightGenerator.generateInsight(getRuleCode(), ctx.getActorUsername(), ctx.getActorRole(), ctx.getTargetResource(), confidence, contextDetails);

            String recommendedAction = """
                1. Apply temporary connection rate-limit or IP ban at edge firewall.
                2. Notify account owner of targeted credential guessing attempts.
                3. Audit whether any attempts from this IP succeeded prior to detection.
                4. Advise user to reset password upon next valid login.
                """.trim();

            return DetectionResult.detected(getRuleCode(), title, getCategory(), severity, confidence, reason, evidenceJson, insight, recommendedAction);
        }

        return DetectionResult.safe();
    }
}
