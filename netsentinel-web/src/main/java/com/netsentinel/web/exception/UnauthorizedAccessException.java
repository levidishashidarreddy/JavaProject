package com.netsentinel.web.exception;

/**
 * Exception thrown when a user attempts to access a resource
 * outside their permitted scope (e.g., student accessing another student's marks).
 */
public class UnauthorizedAccessException extends NetSentinelException {

    private final String attemptedResource;
    private final String actorUsername;

    public UnauthorizedAccessException(String message, String actorUsername, String attemptedResource) {
        super(message, "ERR_UNAUTHORIZED_ACCESS");
        this.actorUsername = actorUsername;
        this.attemptedResource = attemptedResource;
    }

    public String getAttemptedResource() {
        return attemptedResource;
    }

    public String getActorUsername() {
        return actorUsername;
    }
}

