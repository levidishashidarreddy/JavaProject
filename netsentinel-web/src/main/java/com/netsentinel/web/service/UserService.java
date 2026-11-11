package com.netsentinel.web.service;

import com.netsentinel.web.exception.ResourceNotFoundException;
import com.netsentinel.web.exception.SecurityViolationException;
import com.netsentinel.web.model.User;
import com.netsentinel.web.model.UserRole;
import com.netsentinel.web.repository.UserRepository;
import com.netsentinel.web.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing user identity, provisioning, and role authorizations.
 * 
 * Demonstrates:
 * - Server-side privilege and role escalation verification (RULE 3)
 * - Custom exception throwing (SecurityViolationException, ResourceNotFoundException)
 * - Safe transaction boundaries
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final ActivityLogService activityLogService;

    public UserService(UserRepository userRepository, 
                       PasswordHasher passwordHasher, 
                       ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.activityLogService = activityLogService;
    }

    public List<User> searchUsers(String query, String roleFilter) {
        return userRepository.searchUsers(query, roleFilter);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public Long createDemoUser(String username, String password, String fullName, 
                               String email, String roleStr, String department, 
                               Long adminId, String adminUsername, String ip) {
        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(passwordHasher.hash(password));
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setRole(UserRole.fromString(roleStr));
        user.setDepartment(department.trim());
        user.setStatus("ACTIVE");
        user.setUserCode(user.getRole().name().substring(0, 3) + "-" + System.currentTimeMillis() % 10000);

        Long newId = userRepository.insert(user);
        activityLogService.logAllowed(adminId, adminUsername, "ADMIN", "USER_CREATE",
                "USER", String.valueOf(newId), "/admin/users/add", ip,
                "Provisioned new demo user " + username + " with role " + roleStr);
        return newId;
    }

    /**
     * Enforce RULE 3: Unauthorized Privilege Changes.
     * Only ADMIN accounts can modify user roles.
     */
    public boolean updateUserRole(Long actorId, String actorUsername, String actorRole, 
                                  Long targetUserId, String newRoleStr, String ip) {
        Optional<User> targetUserOpt = userRepository.findById(targetUserId);
        if (targetUserOpt.isEmpty()) {
            throw new ResourceNotFoundException("User", targetUserId);
        }

        User targetUser = targetUserOpt.get();
        String oldRole = targetUser.getRole() != null ? targetUser.getRole().name() : "UNKNOWN";

        // Non-administrative users attempting role modification trigger RULE-PRV-003
        if (!"ADMIN".equalsIgnoreCase(actorRole)) {
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("target_user", targetUser.getUsername());
            evidence.put("old_role", oldRole);
            evidence.put("requested_role", newRoleStr);
            evidence.put("simulation_privilege_escalation", true);

            activityLogService.logDenied(actorId, actorUsername, actorRole, "USER_ROLE_CHANGE_UNAUTHORIZED",
                    "USER_ROLE", String.valueOf(targetUserId), "/admin/users/" + targetUserId + "/role", ip,
                    String.format("Non-admin actor '%s' attempted to elevate or mutate role of '%s' to '%s'", 
                            actorUsername, targetUser.getUsername(), newRoleStr),
                    evidence);

            throw new SecurityViolationException(
                    String.format("Access Denied: Account '%s' (%s) lacks authorization to modify user roles.", actorUsername, actorRole),
                    "RULE-PRV-003", "CRITICAL");
        }

        UserRole role = UserRole.fromString(newRoleStr);
        if (role != null) {
            userRepository.updateRole(targetUserId, role);
            activityLogService.logAllowed(actorId, actorUsername, "ADMIN", "USER_ROLE_CHANGE",
                    "USER_ROLE", String.valueOf(targetUserId), "/admin/users/" + targetUserId + "/role", ip,
                    String.format("Updated user '%s' (ID %d) role from %s to %s", targetUser.getUsername(), targetUserId, oldRole, newRoleStr));
            return true;
        }

        return false;
    }

    /**
     * Toggle user status between ACTIVE and INACTIVE.
     */
    public boolean toggleUserStatus(Long actorId, String actorUsername, String actorRole, 
                                    Long targetUserId, String ip) {
        Optional<User> targetUserOpt = userRepository.findById(targetUserId);
        if (targetUserOpt.isEmpty()) {
            throw new ResourceNotFoundException("User", targetUserId);
        }

        User targetUser = targetUserOpt.get();

        if (!"ADMIN".equalsIgnoreCase(actorRole)) {
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("target_user", targetUser.getUsername());
            evidence.put("simulation_privilege_escalation", true);

            activityLogService.logDenied(actorId, actorUsername, actorRole, "USER_STATUS_CHANGE_UNAUTHORIZED",
                    "USER_STATUS", String.valueOf(targetUserId), "/admin/users/" + targetUserId + "/toggle-status", ip,
                    String.format("Non-admin actor '%s' attempted to toggle account status of '%s'", actorUsername, targetUser.getUsername()),
                    evidence);

            throw new SecurityViolationException("Access Denied: Only administrators can modify account status.", "RULE-PRV-003", "CRITICAL");
        }

        String newStatus = "ACTIVE".equalsIgnoreCase(targetUser.getStatus()) ? "INACTIVE" : "ACTIVE";
        userRepository.updateStatus(targetUserId, newStatus);

        activityLogService.logAllowed(actorId, actorUsername, "ADMIN", "USER_STATUS_CHANGE",
                "USER_STATUS", String.valueOf(targetUserId), "/admin/users/" + targetUserId + "/toggle-status", ip,
                String.format("Toggled status of user '%s' to %s", targetUser.getUsername(), newStatus));
        return true;
    }

    public int countTotalUsers() {
        return userRepository.countTotal();
    }
}

