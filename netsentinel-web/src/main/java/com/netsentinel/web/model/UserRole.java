package com.netsentinel.web.model;

public enum UserRole {
    STUDENT,
    FACULTY,
    ADMIN;

    public static UserRole fromString(String role) {
        if (role == null) return null;
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

