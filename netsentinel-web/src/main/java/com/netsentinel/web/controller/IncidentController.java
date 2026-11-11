package com.netsentinel.web.controller;

import com.netsentinel.web.engine.insight.SentinelInsightService;
import com.netsentinel.web.model.Incident;
import com.netsentinel.web.model.IncidentEvent;
import com.netsentinel.web.security.UserSession;
import com.netsentinel.web.service.IncidentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final SentinelInsightService insightService;

    public IncidentController(IncidentService incidentService, SentinelInsightService insightService) {
        this.incidentService = incidentService;
        this.insightService = insightService;
    }

    @GetMapping
    public String listIncidents(@RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "category", required = false) String category,
                                @RequestParam(value = "severity", required = false) String severity,
                                @RequestParam(value = "status", required = false) String status,
                                @RequestParam(value = "page", defaultValue = "1") int page,
                                @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                Model model) {
        List<Incident> incidents = incidentService.searchIncidents(search, category, severity, status, page, pageSize);
        int totalCount = incidentService.countSearchIncidents(search, category, severity, status);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        model.addAttribute("incidents", incidents);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("severity", severity);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", Math.max(1, totalPages));
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("stats", incidentService.getStatsSummary());

        return "admin/incidents";
    }

    @GetMapping("/{id}")
    public String viewIncident(@PathVariable("id") Long id, Model model) {
        Optional<Incident> incidentOpt = incidentService.getIncidentById(id);
        if (incidentOpt.isEmpty()) {
            return "redirect:/admin/incidents?error=not_found";
        }

        Incident incident = incidentOpt.get();
        List<IncidentEvent> events = incidentService.getIncidentEvents(id);

        model.addAttribute("incident", incident);
        model.addAttribute("events", events);
        insightService.generateDossier(id).ifPresent(dossier -> model.addAttribute("dossier", dossier));

        return "admin/incident-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String newStatus,
                               @RequestParam(value = "notes", required = false) String notes,
                               HttpSession session,
                               HttpServletRequest request) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        String reviewer = (user != null) ? user.getUsername() : "admin01";
        Long reviewerId = (user != null) ? user.getId() : 4L;
        incidentService.updateIncidentStatus(id, newStatus, notes, reviewer, reviewerId, request.getRemoteAddr());

        return "redirect:/admin/incidents/" + id + "?success=status_updated";
    }
}
