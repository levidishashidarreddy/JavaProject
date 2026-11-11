package com.netsentinel.web.service;

import com.netsentinel.web.model.Notification;
import com.netsentinel.web.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getUnreadNotifications(String role) {
        return notificationRepository.findUnreadForRole(role);
    }

    public List<Notification> getAllNotifications(String role, int limit) {
        return notificationRepository.findAllForRole(role, limit);
    }

    public int getUnreadCount(String role) {
        return notificationRepository.countUnreadForRole(role);
    }

    public void markAsRead(Long id) {
        notificationRepository.markAsRead(id);
    }

    public void markAllAsRead(String role) {
        notificationRepository.markAllAsRead(role);
    }
}

