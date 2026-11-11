package com.netsentinel.web.repository;

import com.netsentinel.web.model.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class NotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Notification> notificationRowMapper = (rs, rowNum) -> {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setRecipientRole(rs.getString("recipient_role"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setSeverity(rs.getString("severity"));
        n.setReadStatus(rs.getInt("read_status") == 1);
        n.setLink(rs.getString("link"));

        String dt = rs.getString("created_at");
        if (dt != null) {
            try {
                if (dt.length() > 19) dt = dt.substring(0, 19);
                n.setCreatedAt(LocalDateTime.parse(dt.replace(" ", "T")));
            } catch (Exception ignored) {
                n.setCreatedAt(LocalDateTime.now());
            }
        }
        return n;
    };

    public Long insert(Notification n) {
        String sql = """
            INSERT INTO notifications (recipient_role, title, message, severity, read_status, link, created_at)
            VALUES (?, ?, ?, ?, ?, ?, datetime('now'))
        """;
        jdbcTemplate.update(sql,
                n.getRecipientRole() != null ? n.getRecipientRole() : "ADMIN",
                n.getTitle(),
                n.getMessage(),
                n.getSeverity() != null ? n.getSeverity() : "INFO",
                n.isReadStatus() ? 1 : 0,
                n.getLink()
        );
        return 1L;
    }

    public List<Notification> findUnreadForRole(String role) {
        String sql = "SELECT * FROM notifications WHERE (recipient_role = ? OR recipient_role = 'ALL') AND read_status = 0 ORDER BY id DESC LIMIT 10";
        return jdbcTemplate.query(sql, notificationRowMapper, role);
    }

    public List<Notification> findAllForRole(String role, int limit) {
        String sql = "SELECT * FROM notifications WHERE recipient_role = ? OR recipient_role = 'ALL' ORDER BY id DESC LIMIT ?";
        return jdbcTemplate.query(sql, notificationRowMapper, role, limit);
    }

    public void markAsRead(Long id) {
        jdbcTemplate.update("UPDATE notifications SET read_status = 1 WHERE id = ?", id);
    }

    public void markAllAsRead(String role) {
        jdbcTemplate.update("UPDATE notifications SET read_status = 1 WHERE recipient_role = ? OR recipient_role = 'ALL'", role);
    }

    public int countUnreadForRole(String role) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE (recipient_role = ? OR recipient_role = 'ALL') AND read_status = 0",
                Integer.class, role);
        return count != null ? count : 0;
    }
}
