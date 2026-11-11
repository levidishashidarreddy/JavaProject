package com.netsentinel.web.exception;

/**
 * Exception thrown when an active security violation occurs
 * (e.g., unauthorized attendance mutation or privilege escalation attempt).
 */
public class SecurityViolationException extends NetSentinelException {

    private final String ruleCode;
    private final String severity;

    public SecurityViolationException(String message, String ruleCode, String severity) {
        super(message, "ERR_SECURITY_VIOLATION");
        this.ruleCode = ruleCode;
        this.severity = severity;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getSeverity() {
        return severity;
    }
}

