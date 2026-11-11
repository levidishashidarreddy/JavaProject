package com.netsentinel.web.security;

import com.netsentinel.web.model.User;
import com.netsentinel.web.model.UserRole;

import java.io.Serializable;

public class UserSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userCode;
    private String username;
    private String fullName;
    private String email;
    private UserRole role;
    private String department;

    public UserSession() {}

    public UserSession(User user) {
        this.id = user.getId();
        this.userCode = user.getUserCode();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.department = user.getDepartment();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isStudent() { return role == UserRole.STUDENT; }
    public boolean isFaculty() { return role == UserRole.FACULTY; }
    public boolean isAdmin() { return role == UserRole.ADMIN; }
}

