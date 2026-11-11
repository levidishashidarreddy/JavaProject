package com.netsentinel.web.engine.rules;

import com.netsentinel.web.engine.DetectionContext;
import com.netsentinel.web.engine.DetectionResult;
import com.netsentinel.web.model.DetectionRuleConfig;

/**
 * Common DetectionRule interface.
 * Defines contract for security anomaly detection algorithms.
 */
public interface DetectionRule {
    String getRuleCode();
    String getRuleName();
    String getCategory();
    String getDefaultSeverity();
    DetectionResult evaluate(DetectionContext context, DetectionRuleConfig config);
}
