package com.netsentinel.web.exception;

/**
 * Abstract Base Custom Exception for NetSentinel.
 * 
 * Demonstrates:
 * - Abstract class hierarchy
 * - Inheritance from RuntimeException
 * - Constructor chaining using 'super' and 'this'
 * - Encapsulation of security error codes
 */
public abstract class NetSentinelException extends RuntimeException {

    private final String errorCode;
    private final long timestamp;

    public NetSentinelException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    public NetSentinelException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (timestamp: %d)", errorCode, getMessage(), timestamp);
    }
}

