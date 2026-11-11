package com.netsentinel.web.exception;

/**
 * Exception thrown when an inactive or suspended user attempts authentication or operations.
 */
public class AccountInactiveException extends NetSentinelException {

    private final String username;

    public AccountInactiveException(String username) {
        super(String.format("User account '%s' is inactive or suspended.", username), "ERR_ACCOUNT_INACTIVE");
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}

