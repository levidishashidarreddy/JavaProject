package com.netsentinel.web.service;

import com.netsentinel.web.model.Incident;
import com.netsentinel.web.model.IncidentEvent;
import com.netsentinel.web.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final ActivityLogService activityLogService;

    public IncidentService(IncidentRepository incidentRepository, ActivityLogService activityLogService) {
        this.incidentRepository = incidentRepository;
        this.activityLogService = activityLogService;
    }

    public List<Incident> getRecentIncidents(int limit) {
        return incidentRepository.findRecent(limit);
    }

    public Optional<Incident> getIncidentById(Long id) {
        return incidentRepository.findById(id);
    }

    public Optional<Incident> getIncidentByNumber(String incNumber) {
        return incidentRepository.findByIncidentNumber(incNumber);
    }

    public List<Incident> searchIncidents(String query, String category, String severity, String status, int page, int pageSize) {
        int offset = Math.max(0, (page - 1) * pageSize);
        return incidentRepository.searchIncidents(query, category, severity, status, pageSize, offset);
    }

    public int countSearchIncidents(String query, String category, String severity, String status) {
        return incidentRepository.countSearchIncidents(query, category, severity, status);
    }

    @Transactional
    public boolean updateIncidentStatus(Long incidentId, String newStatus, String notes, String adminUsername, Long adminId, String ip) {
        Optional<Incident> opt = incidentRepository.findById(incidentId);
        if (opt.isEmpty()) return false;

        Incident inc = opt.get();
        // Record reviewer and review timestamp directly in SQLite
        incidentRepository.updateStatusAndReview(incidentId, newStatus, adminUsername, notes);

        // Record timeline event
        String noteText = "Status changed to " + newStatus + (notes != null && !notes.isBlank() ? ": " + notes.trim() : ".");
        IncidentEvent event = new IncidentEvent(incidentId, "STATUS_UPDATE", adminId, adminUsername, noteText, LocalDateTime.now());
        incidentRepository.insertEvent(event);

        activityLogService.logAllowed(adminId, adminUsername, "ADMIN", "INCIDENT_UPDATE",
                "INCIDENT", String.valueOf(incidentId), "/admin/incidents/" + incidentId, ip,
                "Updated incident " + inc.getIncidentNumber() + " status to " + newStatus);
        return true;
    }

    public List<IncidentEvent> getIncidentEvents(Long incidentId) {
        return incidentRepository.findEventsByIncidentId(incidentId);
    }

    public Map<String, Object> getStatsSummary() {
        return incidentRepository.getStatsSummary();
    }
}
