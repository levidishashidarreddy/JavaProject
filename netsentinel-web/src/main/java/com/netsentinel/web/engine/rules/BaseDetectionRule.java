package com.netsentinel.web.engine.rules;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.DetectionResult;
import com.netsentinel.web.engine.SentinelInsightGenerator;
import com.netsentinel.web.model.DetectionRuleConfig;

/**
 * Abstract Base Class for NetSentinel Detection Rules.
 * 
 * Demonstrates:
 * - Abstract class and interface implementation (DetectionRule)
 * - Encapsulation using private/protected fields
 * - 'this', 'static', and 'final' keywords
 * - Constructor chaining
 * - Template Method Pattern (evaluate() method coordinates lifecycle and calls abstract evaluateRule())
 * - Method overriding
 * - Clean syllabus-aligned object-oriented design
 */
public abstract class BaseDetectionRule implements DetectionRule {

    private final String ruleCode;
    private final String ruleName;
    private final String category;
    private final String defaultSeverity;
    protected final SentinelInsightGenerator insightGenerator;

    public BaseDetectionRule(String ruleCode, String ruleName, String category, 
                             String defaultSeverity, SentinelInsightGenerator insightGenerator) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.category = category;
        this.defaultSeverity = defaultSeverity;
        this.insightGenerator = insightGenerator;
    }

    @Override
    public final String getRuleCode() {
        return this.ruleCode;
    }

    @Override
    public final String getRuleName() {
        return this.ruleName;
    }

    @Override
    public final String getCategory() {
        return this.category;
    }

    @Override
    public final String getDefaultSeverity() {
        return this.defaultSeverity;
    }

    /**
     * Template method coordinating rule evaluation.
     * Demonstrates dynamic method dispatch by delegating to subclass evaluateRule().
     */
    @Override
    public DetectionResult evaluate(DetectionContext context, DetectionRuleConfig config) {
        if (config != null && !config.isEnabled()) {
            return DetectionResult.safe();
        }

        return evaluateRule(context, config);
    }

    /**
     * Subclass-specific evaluation logic.
     * Demonstrates abstract method implementation in child classes.
     */
    protected abstract DetectionResult evaluateRule(DetectionContext context, DetectionRuleConfig config);

    /**
     * Helper to resolve the active severity (configured vs default).
     */
    protected String resolveSeverity(DetectionRuleConfig config) {
        if (config != null && config.getSeverity() != null && !config.getSeverity().isBlank()) {
            return config.getSeverity();
        }
        return this.defaultSeverity;
    }

    @Override
    public String toString() {
        return String.format("DetectionRule[%s: %s (%s)]", ruleCode, ruleName, defaultSeverity);
    }
}

