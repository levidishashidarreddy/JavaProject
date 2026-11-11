package com.netsentinel.web.service;

import com.netsentinel.web.model.ActivityLog;
import com.netsentinel.web.model.Incident;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AuditLogService acts as a backward-compatible adapter delegating
 * to the unified ActivityLogService.
 */
@Service
public class AuditLogService {

    private final ActivityLogService activityLogService;

    public AuditLogService(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    public Optional<Incident> logAndInspect(Long actorId, String username, String role, String action, 
                                            String resource, String outcome, String ip, String userAgent, 
                                            String details, Map<String, Object> extraAttrs) {
        return activityLogService.logAndInspect(actorId, username, role, action, resource, outcome, ip, userAgent, details, extraAttrs);
    }

    public void log(Long actorId, String username, String role, String action, String resource, String outcome, String ip, String userAgent, String details) {
        activityLogService.log(actorId, username, role, action, resource, outcome, ip, userAgent, details);
    }

    public List<ActivityLog> getRecentLogs(int limit) {
        return activityLogService.getRecentLogs(limit);
    }

    public List<ActivityLog> getRecentLogsByActor(String username, int limit) {
        return activityLogService.getRecentLogsByActor(username, limit);
    }

    public List<ActivityLog> searchLogs(String actor, String role, String action, String outcome, int page, int pageSize) {
        return activityLogService.searchLogs(actor, role, action, outcome, page, pageSize);
    }

    public int countSearchLogs(String actor, String role, String action, String outcome) {
        return activityLogService.countSearchLogs(actor, role, action, outcome);
    }

    public List<Map<String, Object>> getActivityCountsByHour() {
        return activityLogService.getActivityCountsByHour();
    }

    public List<Map<String, Object>> getActivityDistributionByAction() {
        return activityLogService.getActivityDistributionByAction();
    }

    public int countTotalLogs() {
        return activityLogService.countTotalLogs();
    }
}
