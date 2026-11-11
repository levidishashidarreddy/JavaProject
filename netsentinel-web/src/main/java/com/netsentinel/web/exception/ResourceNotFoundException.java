package com.netsentinel.web.exception;

/**
 * Exception thrown when a requested academic or security entity is not found.
 */
public class ResourceNotFoundException extends NetSentinelException {

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("Resource '%s' with identifier '%s' not found.", resourceType, resourceId), "ERR_RESOURCE_NOT_FOUND");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getResourceId() {
        return resourceId;
    }
}

