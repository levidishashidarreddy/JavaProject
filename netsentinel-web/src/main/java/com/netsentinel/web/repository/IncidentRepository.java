package com.netsentinel.web.repository;

import com.netsentinel.web.model.Incident;
import com.netsentinel.web.model.IncidentEvent;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class IncidentRepository {

    private final JdbcTemplate jdbcTemplate;

    public IncidentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Incident> incidentRowMapper = (rs, rowNum) -> {
        Incident inc = new Incident();
        inc.setId(rs.getLong("id"));
        inc.setIncidentNumber(rs.getString("incident_number"));
        inc.setTitle(rs.getString("title"));
        inc.setCategory(rs.getString("category"));
        inc.setSeverity(rs.getString("severity"));
        String rawStatus = rs.getString("status");
        inc.setStatus("INVESTIGATING".equalsIgnoreCase(rawStatus) ? "UNDER_INVESTIGATION" : rawStatus);

        long actorId = rs.getLong("actor_id");
        if (!rs.wasNull()) inc.setActorId(actorId);

        inc.setActorUsername(rs.getString("actor_username"));
        inc.setActorRole(rs.getString("actor_role"));
        inc.setTargetResource(rs.getString("target_resource"));
        inc.setRuleCode(rs.getString("rule_code"));
        inc.setConfidenceScore(rs.getInt("confidence_score"));
        inc.setReason(rs.getString("reason"));
        inc.setEvidenceJson(rs.getString("evidence_json"));
        inc.setInsightExplanation(rs.getString("insight_explanation"));
        inc.setRecommendedAction(rs.getString("recommended_action"));
        inc.setAssignedTo(rs.getString("assigned_to"));
        
        try { inc.setReviewedBy(rs.getString("reviewed_by")); } catch (Exception ignored) {}
        try { inc.setInvestigationNotes(rs.getString("investigation_notes")); } catch (Exception ignored) {}
        try {
            String rev = rs.getString("reviewed_at");
            if (rev != null) {
                if (rev.length() > 19) rev = rev.substring(0, 19);
                inc.setReviewedAt(LocalDateTime.parse(rev.replace(" ", "T")));
            }
        } catch (Exception ignored) {}

        String created = rs.getString("created_at");
        if (created != null) {
            try {
                if (created.length() > 19) created = created.substring(0, 19);
                inc.setCreatedAt(LocalDateTime.parse(created.replace(" ", "T")));
            } catch (Exception ignored) {
                inc.setCreatedAt(LocalDateTime.now());
            }
        }
        String updated = rs.getString("updated_at");
        if (updated != null) {
            try {
                if (updated.length() > 19) updated = updated.substring(0, 19);
                inc.setUpdatedAt(LocalDateTime.parse(updated.replace(" ", "T")));
            } catch (Exception ignored) {
                inc.setUpdatedAt(LocalDateTime.now());
            }
        }
        return inc;
    };

    private final RowMapper<IncidentEvent> eventRowMapper = (rs, rowNum) -> {
        IncidentEvent ev = new IncidentEvent();
        ev.setId(rs.getLong("id"));
        ev.setIncidentId(rs.getLong("incident_id"));
        ev.setEventType(rs.getString("event_type"));

        long actorId = rs.getLong("actor_id");
        if (!rs.wasNull()) ev.setActorId(actorId);

        ev.setActorUsername(rs.getString("actor_username"));
        ev.setNotes(rs.getString("notes"));

        String ts = rs.getString("timestamp");
        if (ts != null) {
            try {
                if (ts.length() > 19) ts = ts.substring(0, 19);
                ev.setTimestamp(LocalDateTime.parse(ts.replace(" ", "T")));
            } catch (Exception ignored) {
                ev.setTimestamp(LocalDateTime.now());
            }
        }
        return ev;
    };

    public Long insert(Incident incident) {
        String dbStatus = (incident.getStatus() != null && incident.getStatus().equalsIgnoreCase("UNDER_INVESTIGATION"))
                ? "INVESTIGATING" : (incident.getStatus() != null ? incident.getStatus().toUpperCase() : "OPEN");
        String sql = """
            INSERT INTO incidents (incident_number, title, category, severity, status, actor_id, 
                                   actor_username, actor_role, target_resource, rule_code, 
                                   confidence_score, reason, evidence_json, insight_explanation, 
                                   recommended_action, assigned_to, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
        """;
        jdbcTemplate.update(sql,
                incident.getIncidentNumber(),
                incident.getTitle(),
                incident.getCategory(),
                incident.getSeverity(),
                dbStatus,
                incident.getActorId(),
                incident.getActorUsername(),
                incident.getActorRole(),
                incident.getTargetResource(),
                incident.getRuleCode(),
                incident.getConfidenceScore() != null ? incident.getConfidenceScore() : 85,
                incident.getReason(),
                incident.getEvidenceJson(),
                incident.getInsightExplanation(),
                incident.getRecommendedAction(),
                incident.getAssignedTo() != null ? incident.getAssignedTo() : "Admin Dave"
        );
        Long id = jdbcTemplate.queryForObject("SELECT id FROM incidents WHERE incident_number = ?", Long.class, incident.getIncidentNumber());
        incident.setId(id);
        return id;
    }

    public Optional<Incident> findById(Long id) {
        String sql = "SELECT * FROM incidents WHERE id = ?";
        try {
            Incident inc = jdbcTemplate.queryForObject(sql, incidentRowMapper, id);
            return Optional.ofNullable(inc);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Incident> findByIncidentNumber(String number) {
        String sql = "SELECT * FROM incidents WHERE incident_number = ? LIMIT 1";
        try {
            Incident inc = jdbcTemplate.queryForObject(sql, incidentRowMapper, number);
            return Optional.ofNullable(inc);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Incident> findRecent(int limit) {
        return jdbcTemplate.query("SELECT * FROM incidents ORDER BY id DESC LIMIT ?", incidentRowMapper, limit);
    }

    public List<Incident> searchIncidents(String query, String category, String severity, String status, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM incidents WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(incident_number) LIKE ? OR LOWER(title) LIKE ? OR LOWER(actor_username) LIKE ? OR LOWER(target_resource) LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            sql.append("AND category = ? ");
            params.add(category.trim().toUpperCase());
        }
        if (severity != null && !severity.isBlank() && !"ALL".equalsIgnoreCase(severity)) {
            sql.append("AND severity = ? ");
            params.add(severity.trim().toUpperCase());
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            if ("UNDER_INVESTIGATION".equalsIgnoreCase(status) || "INVESTIGATING".equalsIgnoreCase(status)) {
                sql.append("AND status IN ('INVESTIGATING', 'UNDER_INVESTIGATION') ");
            } else {
                sql.append("AND status = ? ");
                params.add(status.trim().toUpperCase());
            }
        }

        sql.append("ORDER BY id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), incidentRowMapper, params.toArray());
    }

    public int countSearchIncidents(String query, String category, String severity, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM incidents WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(incident_number) LIKE ? OR LOWER(title) LIKE ? OR LOWER(actor_username) LIKE ? OR LOWER(target_resource) LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            sql.append("AND category = ? ");
            params.add(category.trim().toUpperCase());
        }
        if (severity != null && !severity.isBlank() && !"ALL".equalsIgnoreCase(severity)) {
            sql.append("AND severity = ? ");
            params.add(severity.trim().toUpperCase());
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            if ("UNDER_INVESTIGATION".equalsIgnoreCase(status) || "INVESTIGATING".equalsIgnoreCase(status)) {
                sql.append("AND status IN ('INVESTIGATING', 'UNDER_INVESTIGATION') ");
            } else {
                sql.append("AND status = ? ");
                params.add(status.trim().toUpperCase());
            }
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    public void updateStatus(Long id, String status, String assignedTo) {
        String dbStatus = ("UNDER_INVESTIGATION".equalsIgnoreCase(status)) ? "INVESTIGATING" : (status != null ? status.toUpperCase() : "OPEN");
        String sql = "UPDATE incidents SET status = ?, assigned_to = ?, updated_at = datetime('now') WHERE id = ?";
        jdbcTemplate.update(sql, dbStatus, assignedTo, id);
    }

    public void updateStatusAndReview(Long id, String status, String reviewedBy, String notes) {
        String dbStatus = ("UNDER_INVESTIGATION".equalsIgnoreCase(status)) ? "INVESTIGATING" : (status != null ? status.toUpperCase() : "OPEN");
        String sql = "UPDATE incidents SET status = ?, reviewed_by = ?, investigation_notes = ?, reviewed_at = datetime('now'), updated_at = datetime('now') WHERE id = ?";
        jdbcTemplate.update(sql, dbStatus, reviewedBy, notes, id);
    }

    public Optional<Incident> findActiveIncidentByRuleAndActor(String ruleCode, String actorUsername, int secondsWindow) {
        String sql = """
            SELECT * FROM incidents 
            WHERE rule_code = ? 
              AND LOWER(actor_username) = LOWER(?) 
              AND status IN ('OPEN', 'INVESTIGATING', 'UNDER_INVESTIGATION')
              AND created_at >= datetime('now', '-' || ? || ' seconds')
            ORDER BY id DESC LIMIT 1
        """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, incidentRowMapper, ruleCode, actorUsername, secondsWindow));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Map<String, Object> getStatsSummary() {
        String sql = """
            SELECT 
                COUNT(*) AS total_incidents,
                COALESCE(SUM(CASE WHEN severity IN ('HIGH', 'CRITICAL') THEN 1 ELSE 0 END), 0) AS high_critical_count,
                COALESCE(SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END), 0) AS open_count,
                COALESCE(SUM(CASE WHEN status IN ('INVESTIGATING', 'UNDER_INVESTIGATION') THEN 1 ELSE 0 END), 0) AS investigating_count,
                COALESCE(SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END), 0) AS resolved_count
            FROM incidents
        """;
        try {
            return jdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("total_incidents", 0);
            fallback.put("high_critical_count", 0);
            fallback.put("open_count", 0);
            fallback.put("investigating_count", 0);
            fallback.put("resolved_count", 0);
            return fallback;
        }
    }

    public List<Map<String, Object>> getSeverityDistribution() {
        String sql = """
            SELECT severity, COUNT(*) AS count
            FROM incidents
            GROUP BY severity
            ORDER BY CASE severity 
                WHEN 'CRITICAL' THEN 1 
                WHEN 'HIGH' THEN 2 
                WHEN 'MEDIUM' THEN 3 
                WHEN 'LOW' THEN 4 
                ELSE 5 END
        """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getCategoryDistribution() {
        String sql = """
            SELECT category, COUNT(*) AS count
            FROM incidents
            GROUP BY category
            ORDER BY count DESC
        """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getTopTriggeredRules(int limit) {
        String sql = """
            SELECT rule_code, COUNT(*) AS count, MAX(title) AS title, MAX(severity) AS severity
            FROM incidents
            GROUP BY rule_code
            ORDER BY count DESC
            LIMIT ?
        """;
        return jdbcTemplate.queryForList(sql, limit);
    }

    public List<Map<String, Object>> getIncidentsByStatusDistribution() {
        String sql = """
            SELECT status, COUNT(*) AS count
            FROM incidents
            GROUP BY status
            ORDER BY count DESC
        """;
        return jdbcTemplate.queryForList(sql);
    }

    // Incident Timeline Events
    public Long insertEvent(IncidentEvent event) {
        String sql = "INSERT INTO incident_events (incident_id, event_type, actor_id, actor_username, notes, timestamp) VALUES (?, ?, ?, ?, ?, datetime('now'))";
        jdbcTemplate.update(sql, event.getIncidentId(), event.getEventType(), event.getActorId(), event.getActorUsername(), event.getNotes());
        return 1L;
    }

    public List<IncidentEvent> findEventsByIncidentId(Long incidentId) {
        String sql = "SELECT * FROM incident_events WHERE incident_id = ? ORDER BY id ASC";
        return jdbcTemplate.query(sql, eventRowMapper, incidentId);
    }

    public int countTotal() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM incidents", Integer.class);
        return count != null ? count : 0;
    }
}
