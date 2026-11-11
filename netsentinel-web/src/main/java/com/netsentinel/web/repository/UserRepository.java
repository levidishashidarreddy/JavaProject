package com.netsentinel.web.repository;

import com.netsentinel.web.model.User;
import com.netsentinel.web.model.UserRole;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUserCode(rs.getString("user_code"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setRole(UserRole.fromString(rs.getString("role")));
        user.setDepartment(rs.getString("department"));
        user.setStatus(rs.getString("status"));
        
        String createdStr = rs.getString("created_at");
        if (createdStr != null) {
            try {
                if (createdStr.length() > 19) createdStr = createdStr.substring(0, 19);
                user.setCreatedAt(LocalDateTime.parse(createdStr.replace(" ", "T")));
            } catch (Exception ignored) {
                user.setCreatedAt(LocalDateTime.now());
            }
        }
        return user;
    };

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, username);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<User> findAll() {
        return jdbcTemplate.query("SELECT * FROM users ORDER BY role ASC, full_name ASC", userRowMapper);
    }

    public List<User> findByRole(UserRole role) {
        return jdbcTemplate.query("SELECT * FROM users WHERE role = ? ORDER BY full_name ASC", userRowMapper, role.name());
    }

    public List<User> searchUsers(String query, String roleFilter) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1 ");
        if (roleFilter != null && !roleFilter.isBlank() && !"ALL".equalsIgnoreCase(roleFilter)) {
            sql.append("AND role = '").append(roleFilter.toUpperCase()).append("' ");
        }
        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(username) LIKE '").append(q)
               .append("' OR LOWER(full_name) LIKE '").append(q)
               .append("' OR LOWER(email) LIKE '").append(q)
               .append("' OR LOWER(user_code) LIKE '").append(q).append("') ");
        }
        sql.append("ORDER BY id ASC");
        return jdbcTemplate.query(sql.toString(), userRowMapper);
    }

    public Long insert(User user) {
        String sql = """
            INSERT INTO users (user_code, username, password_hash, full_name, email, role, department, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
        """;
        jdbcTemplate.update(sql,
                user.getUserCode(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getDepartment(),
                user.getStatus() != null ? user.getStatus() : "ACTIVE"
        );
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, user.getUsername());
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE users SET status = ? WHERE id = ?", status, id);
    }

    public void updateRole(Long id, UserRole role) {
        jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", role.name(), id);
    }

    public int countTotal() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        return count != null ? count : 0;
    }
}
