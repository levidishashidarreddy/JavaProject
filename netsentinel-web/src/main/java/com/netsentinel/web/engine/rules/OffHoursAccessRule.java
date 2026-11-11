package com.netsentinel.web.engine.rules;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.DetectionResult;
import com.netsentinel.web.engine.SentinelInsightGenerator;
import com.netsentinel.web.model.DetectionRuleConfig;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * RULE 5 — Anomalous Off-Hours Portal Access (RULE-TIM-005)
 * 
 * Category: ANOMALY
 * Severity: MEDIUM
 */
@Component
public class OffHoursAccessRule extends BaseDetectionRule {

    public OffHoursAccessRule(SentinelInsightGenerator insightGenerator) {
        super("RULE-TIM-005", "Anomalous Off-Hours Portal Access", "ANOMALY", "MEDIUM", insightGenerator);
    }

    @Override
    protected DetectionResult evaluateRule(DetectionContext ctx, DetectionRuleConfig config) {
        boolean isSimulation = Boolean.TRUE.equals(ctx.getAttribute("simulation_off_hours"));
        LocalTime now = LocalTime.now();
        boolean isOffHours = now.isAfter(LocalTime.of(2, 0)) && now.isBefore(LocalTime.of(5, 0));
        boolean isAdministrativeAction = ctx.getAction().contains("RECORD") || 
                                         ctx.getAction().contains("ADMIN") ||
                                         ctx.getAction().contains("UPDATE");

        if ((isOffHours && isAdministrativeAction) || isSimulation) {
            String title = "Anomalous Off-Hours Academic Operation";
            String severity = resolveSeverity(config);
            int confidence = 82;

            String reason = String.format("Actor '%s' executed academic mutation action '%s' during quiet-hours window (02:00 - 05:00 AM).",
                    ctx.getActorUsername(), ctx.getAction());

            String evidenceJson = String.format("""
                {"actor": "%s", "action": "%s", "time": "%s", "window": "02:00-05:00", "ip_address": "%s"}
                """.trim(),
                    ctx.getActorUsername(), ctx.getAction(), now.toString(), ctx.getIpAddress()
            );

            String contextDetails = String.format(
                "- Execution Time: %s (Standard quiet window: 02:00 - 05:00)\n- Action: %s\n- Actor IP: %s",
                now.toString(), ctx.getAction(), ctx.getIpAddress()
            );

            String insight = insightGenerator.generateInsight(getRuleCode(), ctx.getActorUsername(), ctx.getActorRole(), ctx.getTargetResource(), confidence, contextDetails);

            String recommendedAction = """
                1. Verify if user is working under approved emergency maintenance or time-zone offset.
                2. Audit concurrent sessions from this IP.
                3. Require re-authentication on next morning login.
                """.trim();

            return DetectionResult.detected(getRuleCode(), title, getCategory(), severity, confidence, reason, evidenceJson, insight, recommendedAction);
        }

        return DetectionResult.safe();
    }
}
