package com.netsentinel.web.controller;

import com.netsentinel.web.model.Notification;
import com.netsentinel.web.security.UserSession;
import com.netsentinel.web.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;

    public NotificationApiController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(HttpSession session) {
        UserSession user = (session != null) ? (UserSession) session.getAttribute("userSession") : null;
        String role = (user != null) ? user.getRole().name() : "ADMIN";

        List<Notification> unread = notificationService.getUnreadNotifications(role);
        int count = notificationService.getUnreadCount(role);

        return ResponseEntity.ok(Map.of(
                "unreadCount", count,
                "notifications", unread
        ));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable("id") Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(HttpSession session) {
        UserSession user = (session != null) ? (UserSession) session.getAttribute("userSession") : null;
        String role = (user != null) ? user.getRole().name() : "ADMIN";
        notificationService.markAllAsRead(role);
        return ResponseEntity.ok(Map.of("success", true, "role", role));
    }
}

