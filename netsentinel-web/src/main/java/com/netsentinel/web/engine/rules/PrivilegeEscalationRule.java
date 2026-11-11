package com.netsentinel.web.engine.rules;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.DetectionResult;
import com.netsentinel.web.engine.SentinelInsightGenerator;
import com.netsentinel.web.model.DetectionRuleConfig;
import org.springframework.stereotype.Component;

/**
 * RULE 3 — Unauthorized Privilege Changes (RULE-PRV-003)
 * 
 * Category: ACCESS_CONTROL
 * Severity: CRITICAL
 * 
 * Detection Logic:
 * Checks:
 * 1. Actor's role (Only ADMIN has permission to modify roles or user accounts)
 * 2. Permission to modify roles
 * 3. Target account
 * 4. Old role vs Requested new role
 * 
 * Demonstrates:
 * - Inheritance from BaseDetectionRule
 * - Access control boundary enforcement
 * - Detailed forensic metadata compilation
 */
@Component
public class PrivilegeEscalationRule extends BaseDetectionRule {

    public PrivilegeEscalationRule(SentinelInsightGenerator insightGenerator) {
        super("RULE-PRV-003", "Unauthorized Privilege Changes", "ACCESS_CONTROL", "CRITICAL", insightGenerator);
    }

    @Override
    protected DetectionResult evaluateRule(DetectionContext ctx, DetectionRuleConfig config) {
        boolean isSimulation = Boolean.TRUE.equals(ctx.getAttribute("simulation_privilege_escalation"));
        boolean isNonAdmin = !"ADMIN".equalsIgnoreCase(ctx.getActorRole());

        boolean isRoleChangeAttempt = ctx.getAction().contains("ROLE_CHANGE") || 
                                     ctx.getAction().contains("PRIVILEGE") || 
                                     ctx.getAction().contains("USER_STATUS_CHANGE") ||
                                     ctx.getTargetResource().contains("/users/role") ||
                                     ctx.getTargetResource().contains("/users/status");

        boolean isDirectAdminAccess = ctx.getTargetResource().startsWith("/admin") && isNonAdmin;

        if ((isNonAdmin && (isRoleChangeAttempt || isDirectAdminAccess)) || isSimulation) {
            String title = "Unauthorized Privilege Escalation Intercepted";
            String severity = resolveSeverity(config);
            int confidence = 98;

            String targetUser = (String) ctx.getAttribute("target_user");
            if (targetUser == null) targetUser = ctx.getTargetId() != null ? ctx.getTargetId() : "User Management System";

            String oldRole = (String) ctx.getAttribute("old_role");
            String requestedRole = (String) ctx.getAttribute("requested_role");
            String roleTransition = (oldRole != null && requestedRole != null) ? 
                    String.format("[%s -> %s]", oldRole, requestedRole) : "[ELEVATED_ROLE]";

            String reason = String.format("Actor '%s' with non-administrative role [%s] attempted unauthorized privilege change %s on target '%s'.",
                    ctx.getActorUsername(), ctx.getActorRole(), roleTransition, targetUser);

            String evidenceJson = String.format("""
                {"actor_username": "%s", "actor_role": "%s", "target_account": "%s", "old_role": "%s", "requested_role": "%s", "action": "%s", "target_resource": "%s", "ip_address": "%s", "threat_classification": "HORIZONTAL_OR_VERTICAL_ESCALATION"}
                """.trim(),
                    ctx.getActorUsername(), ctx.getActorRole(), targetUser, 
                    oldRole != null ? oldRole : "N/A", 
                    requestedRole != null ? requestedRole : "ADMIN",
                    ctx.getAction(), ctx.getTargetResource(), ctx.getIpAddress()
            );

            String contextDetails = String.format(
                "- Attempted Target: %s\n- Role Transition Attempt: %s\n- Origin IP: %s\n- Kernel Intervention: Action blocked prior to database commit.",
                targetUser, roleTransition, ctx.getIpAddress()
            );

            String insight = insightGenerator.generateInsight(getRuleCode(), ctx.getActorUsername(), ctx.getActorRole(), ctx.getTargetResource(), confidence, contextDetails);

            String recommendedAction = """
                1. Immediately invalidate the caller's active session and revoke authentication tokens.
                2. Audit web proxy logs for automated privilege probing or modified session parameters.
                3. Inspect adjacent accounts to confirm no other roles were manipulated.
                4. Conduct mandatory security review with the account owner.
                """.trim();

            return DetectionResult.detected(getRuleCode(), title, getCategory(), severity, confidence, reason, evidenceJson, insight, recommendedAction);
        }

        return DetectionResult.safe();
    }
}
