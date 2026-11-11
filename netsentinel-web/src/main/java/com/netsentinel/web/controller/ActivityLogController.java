package com.netsentinel.web.controller;

import com.netsentinel.web.model.ActivityLog;
import com.netsentinel.web.service.AuditLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin/activity-logs")
public class ActivityLogController {

    private final AuditLogService auditLogService;

    public ActivityLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String listLogs(@RequestParam(value = "actor", required = false) String actor,
                           @RequestParam(value = "role", required = false) String role,
                           @RequestParam(value = "action", required = false) String action,
                           @RequestParam(value = "outcome", required = false) String outcome,
                           @RequestParam(value = "page", defaultValue = "1") int page,
                           @RequestParam(value = "pageSize", defaultValue = "15") int pageSize,
                           Model model) {
        List<ActivityLog> logs = auditLogService.searchLogs(actor, role, action, outcome, page, pageSize);
        int totalCount = auditLogService.countSearchLogs(actor, role, action, outcome);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        model.addAttribute("logs", logs);
        model.addAttribute("actor", actor);
        model.addAttribute("role", role);
        model.addAttribute("action", action);
        model.addAttribute("outcome", outcome);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", Math.max(1, totalPages));
        model.addAttribute("totalCount", totalCount);

        return "admin/activity-logs";
    }
}

