package com.netsentinel.web.repository;

import com.netsentinel.web.model.SimulationRun;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SimulationRepository {

    private final JdbcTemplate jdbcTemplate;

    public SimulationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<SimulationRun> simRowMapper = (rs, rowNum) -> {
        SimulationRun run = new SimulationRun();
        run.setId(rs.getLong("id"));
        run.setSimulationType(rs.getString("simulation_type"));
        run.setTriggerPayload(rs.getString("trigger_payload"));
        run.setDetected(rs.getInt("detected") == 1);
        
        long incId = rs.getLong("incident_id");
        if (!rs.wasNull()) run.setIncidentId(incId);
        
        String dt = rs.getString("run_at");
        if (dt != null) {
            try {
                if (dt.length() > 19) dt = dt.substring(0, 19);
                run.setRunAt(LocalDateTime.parse(dt.replace(" ", "T")));
            } catch (Exception ignored) {
                run.setRunAt(LocalDateTime.now());
            }
        }
        run.setSummary(rs.getString("summary"));
        return run;
    };

    public Long recordRun(SimulationRun run) {
        String sql = """
            INSERT INTO simulation_runs (simulation_type, trigger_payload, detected, incident_id, run_at, summary)
            VALUES (?, ?, ?, ?, datetime('now'), ?)
        """;
        jdbcTemplate.update(sql,
                run.getSimulationType(),
                run.getTriggerPayload(),
                run.isDetected() ? 1 : 0,
                run.getIncidentId(),
                run.getSummary()
        );
        return 1L;
    }

    public List<SimulationRun> findRecentRuns(int limit) {
        return jdbcTemplate.query("SELECT * FROM simulation_runs ORDER BY id DESC LIMIT ?", simRowMapper, limit);
    }

    public void clearAll() {
        jdbcTemplate.update("DELETE FROM simulation_runs");
    }
}
