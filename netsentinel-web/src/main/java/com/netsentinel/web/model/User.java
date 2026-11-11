package com.netsentinel.web.model;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String userCode;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private UserRole role;
    private String department;
    private String status;
    private LocalDateTime createdAt;

    public User() {}

    public User(Long id, String userCode, String username, String passwordHash, String fullName, 
                String email, UserRole role, String department, String status, LocalDateTime createdAt) {
        this.id = id;
        this.userCode = userCode;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.department = department;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }
}

