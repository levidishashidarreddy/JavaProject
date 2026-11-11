package com.netsentinel.web.repository;

import com.netsentinel.web.model.DetectionRuleConfig;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class DetectionRuleRepository {

    private final JdbcTemplate jdbcTemplate;

    public DetectionRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<DetectionRuleConfig> ruleRowMapper = (rs, rowNum) -> {
        DetectionRuleConfig cfg = new DetectionRuleConfig();
        cfg.setId(rs.getLong("id"));
        cfg.setRuleCode(rs.getString("rule_code"));
        cfg.setRuleName(rs.getString("rule_name"));
        cfg.setCategory(rs.getString("category"));
        cfg.setDescription(rs.getString("description"));
        cfg.setEnabled(rs.getInt("enabled") == 1);
        cfg.setThreshold(rs.getInt("threshold"));
        cfg.setTimeWindowSeconds(rs.getInt("time_window_seconds"));
        cfg.setSeverity(rs.getString("severity"));

        String dt = rs.getString("updated_at");
        if (dt != null) {
            try {
                if (dt.length() > 19) dt = dt.substring(0, 19);
                cfg.setUpdatedAt(LocalDateTime.parse(dt.replace(" ", "T")));
            } catch (Exception ignored) {
                cfg.setUpdatedAt(LocalDateTime.now());
            }
        }
        return cfg;
    };

    public List<DetectionRuleConfig> findAll() {
        return jdbcTemplate.query("SELECT * FROM detection_rule_configs ORDER BY id ASC", ruleRowMapper);
    }

    public Optional<DetectionRuleConfig> findByCode(String ruleCode) {
        String sql = "SELECT * FROM detection_rule_configs WHERE UPPER(rule_code) = UPPER(?) LIMIT 1";
        try {
            DetectionRuleConfig cfg = jdbcTemplate.queryForObject(sql, ruleRowMapper, ruleCode);
            return Optional.ofNullable(cfg);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void updateRule(String ruleCode, boolean enabled, int threshold, int timeWindowSeconds, String severity) {
        String sql = """
            UPDATE detection_rule_configs 
            SET enabled = ?, threshold = ?, time_window_seconds = ?, severity = ?, updated_at = datetime('now')
            WHERE UPPER(rule_code) = UPPER(?)
        """;
        jdbcTemplate.update(sql, enabled ? 1 : 0, threshold, timeWindowSeconds, severity, ruleCode);
    }

    public void toggleRule(String ruleCode) {
        String sql = "UPDATE detection_rule_configs SET enabled = 1 - enabled, updated_at = datetime('now') WHERE UPPER(rule_code) = UPPER(?)";
        jdbcTemplate.update(sql, ruleCode);
    }
}

