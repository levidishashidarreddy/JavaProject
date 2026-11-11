package com.netsentinel.web.controller;

import com.netsentinel.web.model.Incident;
import com.netsentinel.web.security.UserSession;
import com.netsentinel.web.service.ActivityLogService;
import com.netsentinel.web.service.AnalyticsService;
import com.netsentinel.web.service.IncidentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AnalyticsService analyticsService;
    private final IncidentService incidentService;
    private final ActivityLogService activityLogService;

    public AdminDashboardController(AnalyticsService analyticsService,
                                    IncidentService incidentService,
                                    ActivityLogService activityLogService) {
        this.analyticsService = analyticsService;
        this.incidentService = incidentService;
        this.activityLogService = activityLogService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model, HttpServletRequest request) {
        UserSession user = (UserSession) session.getAttribute("userSession");

        Map<String, Object> metrics = analyticsService.getDashboardMetrics();
        List<Incident> recentIncidents = incidentService.getRecentIncidents(5);

        model.addAttribute("metrics", metrics);
        model.addAttribute("recentIncidents", recentIncidents);
        model.addAttribute("recentActivities", activityLogService.getRecentLogs(8));

        activityLogService.logAllowed(user.getId(), user.getUsername(), user.getRole().name(),
                "VIEW_ADMIN_DASHBOARD", "DASHBOARD", String.valueOf(user.getId()), "/admin/dashboard", request.getRemoteAddr(),
                "Administrator accessed SOC monitoring dashboard");

        return "admin/dashboard";
    }
}
