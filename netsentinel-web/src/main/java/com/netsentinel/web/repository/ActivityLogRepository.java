package com.netsentinel.web.repository;

import com.netsentinel.web.model.ActivityLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class ActivityLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public ActivityLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ActivityLog> logRowMapper = (rs, rowNum) -> {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getLong("id"));
        
        long actorId = rs.getLong("actor_id");
        if (!rs.wasNull()) log.setActorId(actorId);
        
        log.setActorUsername(rs.getString("actor_username"));
        log.setActorRole(rs.getString("actor_role"));
        log.setAction(rs.getString("action"));
        log.setTargetResource(rs.getString("target_resource"));
        
        try { log.setTargetType(rs.getString("target_type")); } catch (Exception ignored) {}
        try { log.setTargetId(rs.getString("target_id")); } catch (Exception ignored) {}
        try { log.setRequestId(rs.getString("request_id")); } catch (Exception ignored) {}

        log.setOutcome(rs.getString("outcome"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setUserAgent(rs.getString("user_agent"));
        log.setDetails(rs.getString("details"));

        String ts = rs.getString("timestamp");
        if (ts != null) {
            try {
                if (ts.length() > 19) ts = ts.substring(0, 19);
                log.setTimestamp(LocalDateTime.parse(ts.replace(" ", "T")));
            } catch (Exception ignored) {
                log.setTimestamp(LocalDateTime.now());
            }
        }
        return log;
    };

    public Long log(ActivityLog log) {
        String sql = """
            INSERT INTO activity_logs (actor_id, actor_username, actor_role, action, target_resource, target_type, target_id, request_id, outcome, ip_address, user_agent, details, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
        """;
        jdbcTemplate.update(sql,
                log.getActorId(),
                log.getActorUsername(),
                log.getActorRole(),
                log.getAction(),
                log.getTargetResource(),
                log.getTargetType() != null ? log.getTargetType() : "RESOURCE",
                log.getTargetId(),
                log.getRequestId(),
                log.getOutcome() != null ? log.getOutcome() : "SUCCESS",
                log.getIpAddress() != null ? log.getIpAddress() : "127.0.0.1",
                log.getUserAgent(),
                log.getDetails()
        );
        return 1L;
    }

    public List<ActivityLog> findRecent(int limit) {
        return jdbcTemplate.query("SELECT * FROM activity_logs ORDER BY id DESC LIMIT ?", logRowMapper, limit);
    }

    public List<ActivityLog> findRecentByActor(String username, int limit) {
        return jdbcTemplate.query("SELECT * FROM activity_logs WHERE LOWER(actor_username) = LOWER(?) ORDER BY id DESC LIMIT ?", logRowMapper, username, limit);
    }

    public List<ActivityLog> searchLogs(String actor, String role, String action, String outcome, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM activity_logs WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (actor != null && !actor.isBlank()) {
            sql.append("AND LOWER(actor_username) LIKE ? ");
            params.add("%" + actor.trim().toLowerCase() + "%");
        }
        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
            sql.append("AND actor_role = ? ");
            params.add(role.trim().toUpperCase());
        }
        if (action != null && !action.isBlank() && !"ALL".equalsIgnoreCase(action)) {
            sql.append("AND action = ? ");
            params.add(action.trim());
        }
        if (outcome != null && !outcome.isBlank() && !"ALL".equalsIgnoreCase(outcome)) {
            sql.append("AND outcome = ? ");
            params.add(outcome.trim().toUpperCase());
        }

        sql.append("ORDER BY id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), logRowMapper, params.toArray());
    }

    public int countSearchLogs(String actor, String role, String action, String outcome) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM activity_logs WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (actor != null && !actor.isBlank()) {
            sql.append("AND LOWER(actor_username) LIKE ? ");
            params.add("%" + actor.trim().toLowerCase() + "%");
        }
        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
            sql.append("AND actor_role = ? ");
            params.add(role.trim().toUpperCase());
        }
        if (action != null && !action.isBlank() && !"ALL".equalsIgnoreCase(action)) {
            sql.append("AND action = ? ");
            params.add(action.trim());
        }
        if (outcome != null && !outcome.isBlank() && !"ALL".equalsIgnoreCase(outcome)) {
            sql.append("AND outcome = ? ");
            params.add(outcome.trim().toUpperCase());
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    public int countRecentActionsInWindow(String username, String action, int secondsWindow) {
        String sql = """
            SELECT COUNT(*) FROM activity_logs 
            WHERE LOWER(actor_username) = LOWER(?) 
              AND action = ? 
              AND timestamp >= datetime('now', '-' || ? || ' seconds')
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username, action, secondsWindow);
        return count != null ? count : 0;
    }

    public int countRecentAcademicMutationsInWindow(String username, int secondsWindow) {
        String sql = """
            SELECT COUNT(*) FROM activity_logs 
            WHERE LOWER(actor_username) = LOWER(?) 
              AND (action LIKE '%MARKS%' OR action LIKE '%ATTENDANCE%')
              AND timestamp >= datetime('now', '-' || ? || ' seconds')
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username, secondsWindow);
        return count != null ? count : 0;
    }

    public int countRecentResourceTouchesInWindow(String username, String resourcePrefix, int secondsWindow) {
        String sql = """
            SELECT COUNT(DISTINCT target_resource) FROM activity_logs 
            WHERE LOWER(actor_username) = LOWER(?) 
              AND target_resource LIKE ? 
              AND timestamp >= datetime('now', '-' || ? || ' seconds')
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username, resourcePrefix + "%", secondsWindow);
        return count != null ? count : 0;
    }

    public List<Map<String, Object>> findActivityCountsByHour() {
        String sql = """
            SELECT strftime('%H:00', timestamp) AS hour_slot, COUNT(*) AS count
            FROM activity_logs
            GROUP BY hour_slot
            ORDER BY hour_slot ASC
            LIMIT 24
        """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findActivityDistributionByAction() {
        String sql = """
            SELECT action, COUNT(*) AS count
            FROM activity_logs
            GROUP BY action
            ORDER BY count DESC
            LIMIT 6
        """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getUserWiseActivitySummary(int limit) {
        String sql = """
            SELECT actor_username, actor_role, COUNT(*) AS total_actions,
                   SUM(CASE WHEN outcome IN ('DENIED', 'BLOCKED') THEN 1 ELSE 0 END) AS blocked_count
            FROM activity_logs
            GROUP BY actor_username, actor_role
            ORDER BY total_actions DESC
            LIMIT ?
        """;
        return jdbcTemplate.queryForList(sql, limit);
    }

    public int countTotal() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM activity_logs", Integer.class);
        return count != null ? count : 0;
    }
}
