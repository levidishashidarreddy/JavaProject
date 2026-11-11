package com.netsentinel.web.service;

import com.netsentinel.web.model.User;
import com.netsentinel.web.repository.UserRepository;
import com.netsentinel.web.security.PasswordHasher;
import com.netsentinel.web.security.UserSession;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final ActivityLogService activityLogService;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.activityLogService = activityLogService;
    }

    public Optional<UserSession> authenticate(String username, String password, String ipAddress, String userAgent) {
        if (username == null || username.isBlank() || password == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            activityLogService.log(null, username, "ANONYMOUS", "LOGIN_FAILED", "USER", username, "/login", "BLOCKED", ipAddress, userAgent, "Unknown username attempted", java.util.UUID.randomUUID().toString(), null);
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            activityLogService.log(user.getId(), user.getUsername(), user.getRole().name(), "LOGIN_BLOCKED", "USER", String.valueOf(user.getId()), "/login", "BLOCKED", ipAddress, userAgent, "Account inactive or suspended", java.util.UUID.randomUUID().toString(), null);
            return Optional.empty();
        }

        if (passwordHasher.matches(password, user.getPasswordHash())) {
            activityLogService.logAllowed(user.getId(), user.getUsername(), user.getRole().name(), "AUTH_LOGIN", "USER", String.valueOf(user.getId()), "/login", ipAddress, "Successful authentication");
            return Optional.of(new UserSession(user));
        } else {
            activityLogService.log(user.getId(), user.getUsername(), user.getRole().name(), "LOGIN_FAILED", "USER", String.valueOf(user.getId()), "/login", "BLOCKED", ipAddress, userAgent, "Incorrect password attempt", java.util.UUID.randomUUID().toString(), null);
            return Optional.empty();
        }
    }

    public Optional<UserSession> demoLogin(String roleIdentifier, String ipAddress, String userAgent) {
        String targetUsername = switch (roleIdentifier.trim().toLowerCase()) {
            case "student", "student101" -> "student101";
            case "student102" -> "student102";
            case "faculty_a", "faculty-a", "faculty_alan", "faculty101" -> "faculty101";
            case "faculty_b", "faculty-b", "faculty_sarah", "faculty102" -> "faculty102";
            case "admin", "administrator", "admin01" -> "admin01";
            default -> roleIdentifier.trim();
        };

        Optional<User> userOpt = userRepository.findByUsername(targetUsername);
        if (userOpt.isEmpty()) {
            // Fallback to alias if needed
            String fallback = switch (targetUsername) {
                case "student101" -> "student";
                case "faculty101" -> "faculty_alan";
                case "faculty102" -> "faculty_sarah";
                case "admin01" -> "admin";
                default -> targetUsername;
            };
            userOpt = userRepository.findByUsername(fallback);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            activityLogService.logAllowed(user.getId(), user.getUsername(), user.getRole().name(), "AUTH_DEMO_LOGIN", "USER", String.valueOf(user.getId()), "/login", ipAddress, "Demo Quick-Login authenticated for " + user.getRole());
            return Optional.of(new UserSession(user));
        }

        return Optional.empty();
    }
}
