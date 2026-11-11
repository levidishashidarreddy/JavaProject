package com.netsentinel.web.config;

import com.netsentinel.web.security.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * NetSentinel Database Initializer and Migration Component.
 * 
 * Demonstrates:
 * - ApplicationRunner interface implementation
 * - Defensive SQL migration and schema evolution (ALTER TABLE on SQLite)
 * - Safe idempotent seeding using parameterized queries and checking existence
 * - Preservation of database state across system restarts
 * 
 * Target users:
 * - student101  (Rahul Sharma, CS - student123)
 * - faculty101  (Prof. Alan Turing, CS - faculty123)
 * - faculty102  (Prof. Sarah Jenkins, CS/Math - faculty123)
 * - admin01     (Dave Miller, SOC - admin123)
 * - student102  (Priya Patel, CS - student123)
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordHasher passwordHasher;
    private final ResourceLoader resourceLoader;

    public DatabaseInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate, 
                               PasswordHasher passwordHasher, ResourceLoader resourceLoader) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordHasher = passwordHasher;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting NetSentinel database verification & schema synchronization...");
        try {
            ensurePragmas();
            executeSchemaScript();
            migrateColumns();
            seedCoreDemoUsers();
            cleanupUnnecessaryMockData();
            seedAcademicStructure();
            seedDetectionRuleConfigs();
            log.info("NetSentinel database initialization completed successfully.");
        } catch (Exception e) {
            log.error("Error during NetSentinel database initialization", e);
        }
    }

    private void ensurePragmas() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
        } catch (Exception e) {
            log.warn("Could not set SQLite pragmas: {}", e.getMessage());
        }
    }

    private void executeSchemaScript() {
        try {
            Resource schemaRes = resourceLoader.getResource("classpath:schema.sql");
            if (schemaRes.exists()) {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(schemaRes);
                populator.setContinueOnError(true);
                populator.execute(dataSource);
            }
        } catch (Exception e) {
            log.warn("Schema script execution notice: {}", e.getMessage());
        }
    }

    private void migrateColumns() {
        // Ensure new columns exist on existing databases
        addMissingColumn("activity_logs", "target_type", "VARCHAR(64) DEFAULT 'RESOURCE'");
        addMissingColumn("activity_logs", "target_id", "VARCHAR(64)");
        addMissingColumn("activity_logs", "request_id", "VARCHAR(64)");
        addMissingColumn("incidents", "reviewed_by", "VARCHAR(64)");
        addMissingColumn("incidents", "reviewed_at", "DATETIME");
        addMissingColumn("incidents", "investigation_notes", "TEXT");
    }

    private void addMissingColumn(String tableName, String columnName, String columnDef) {
        try {
            Set<String> existingColumns = new HashSet<>();
            jdbcTemplate.query("PRAGMA table_info(" + tableName + ")", rs -> {
                existingColumns.add(rs.getString("name").toLowerCase());
            });

            if (!existingColumns.contains(columnName.toLowerCase())) {
                String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef;
                jdbcTemplate.execute(alterSql);
                log.info("Migrated table '{}': added column '{}'", tableName, columnName);
            }
        } catch (Exception e) {
            log.debug("Column check/alter for {}.{}: {}", tableName, columnName, e.getMessage());
        }
    }

    private void seedCoreDemoUsers() {
        // Generate valid BCrypt hashes
        String studentHash = passwordHasher.hash("student123");
        String facultyHash = passwordHasher.hash("faculty123");
        String adminHash = passwordHasher.hash("admin123");

        // Primary required Phase 2 & 3 demo accounts (5 Canonical Accounts Only)
        upsertUser("STU-2026-101", "student101", studentHash, "Rahul Sharma", "student101@campus.netsentinel.edu", "STUDENT", "Computer Science & Engineering");
        upsertUser("STU-2026-102", "student102", studentHash, "Priya Patel", "student102@campus.netsentinel.edu", "STUDENT", "Computer Science & Engineering");
        upsertUser("FAC-2026-F101", "faculty101", facultyHash, "Prof. Alan Turing", "faculty101@campus.netsentinel.edu", "FACULTY", "Computer Science & Engineering");
        upsertUser("FAC-2026-F102", "faculty102", facultyHash, "Dr. Sarah Jenkins", "faculty102@campus.netsentinel.edu", "FACULTY", "Mathematics & Computing");
        upsertUser("ADM-2026-A001", "admin01", adminHash, "Chief Dave Miller", "admin01@security.netsentinel.edu", "ADMIN", "Cybersecurity Operations Center");
    }

    public void cleanupUnnecessaryMockData() {
        try {
            // Remove alias users not in the 5 canonical demo accounts
            jdbcTemplate.update("""
                DELETE FROM users 
                WHERE LOWER(username) IN ('student', 'faculty_alan', 'faculty_sarah', 'faculty_a', 'faculty_b', 'admin')
            """);
            log.info("Cleaned up legacy alias users. Retained canonical 5 demo accounts.");
        } catch (Exception e) {
            log.debug("Notice during mock data cleanup: {}", e.getMessage());
        }
    }

    private void upsertUser(String userCode, String username, String passwordHash, String fullName, String email, String role, String department) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?)", Integer.class, username);
        if (count == null || count == 0) {
            Integer codeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE LOWER(user_code) = LOWER(?)", Integer.class, userCode);
            String finalCode = (codeCount != null && codeCount > 0) ? userCode + "-" + username : userCode;
            String sql = """
                INSERT INTO users (user_code, username, password_hash, full_name, email, role, department, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', datetime('now'))
            """;
            jdbcTemplate.update(sql, finalCode, username, passwordHash, fullName, email, role, department);
            log.info("Seeded demo user: {} ({}) with code {}", username, role, finalCode);
        }
    }

    private void seedAcademicStructure() {
        // Subjects
        upsertSubject("CS301", "Data Structures & Algorithms", "Computer Science & Engineering", 4, 5, "Fundamental data structures, search algorithms, graph theory.");
        upsertSubject("CS305", "Operating Systems & Kernels", "Computer Science & Engineering", 4, 5, "Processes, memory paging, file systems, IPC, concurrency.");
        upsertSubject("CS402", "Network Security & Cryptography", "Computer Science & Engineering", 3, 7, "Ciphers, TLS/SSL, PKI, intrusion detection, firewalls.");
        upsertSubject("CS405", "Database Management Systems", "Computer Science & Engineering", 3, 5, "Relational algebra, SQL indexing, transactions, ACID locks.");

        // Faculty assignments
        Long fac101Id = getUserIdByUsername("faculty101");
        Long fac102Id = getUserIdByUsername("faculty102");
        Long cs301Id = getSubjectIdByCode("CS301");
        Long cs305Id = getSubjectIdByCode("CS305");
        Long cs402Id = getSubjectIdByCode("CS402");
        Long cs405Id = getSubjectIdByCode("CS405");

        if (fac101Id != null) {
            if (cs301Id != null) assignFaculty(fac101Id, cs301Id, "Fall 2026", "2026-2027");
            if (cs402Id != null) assignFaculty(fac101Id, cs402Id, "Fall 2026", "2026-2027");
        }
        if (fac102Id != null) {
            if (cs305Id != null) assignFaculty(fac102Id, cs305Id, "Fall 2026", "2026-2027");
            if (cs405Id != null) assignFaculty(fac102Id, cs405Id, "Fall 2026", "2026-2027");
        }

        // Student enrollments
        Long stu101Id = getUserIdByUsername("student101");
        Long stu102Id = getUserIdByUsername("student102");

        if (stu101Id != null) {
            if (cs301Id != null) enrollStudent(stu101Id, cs301Id, "Fall 2026");
            if (cs305Id != null) enrollStudent(stu101Id, cs305Id, "Fall 2026");
            if (cs402Id != null) enrollStudent(stu101Id, cs402Id, "Fall 2026");
            if (cs405Id != null) enrollStudent(stu101Id, cs405Id, "Fall 2026");
        }

        if (stu102Id != null) {
            // student102 is enrolled in CS301 (faculty101) and CS305 (faculty102)
            if (cs301Id != null) enrollStudent(stu102Id, cs301Id, "Fall 2026");
            if (cs305Id != null) enrollStudent(stu102Id, cs305Id, "Fall 2026");
        }
    }

    private void seedDetectionRuleConfigs() {
        upsertRuleConfig("RULE-ATT-001", "Unauthorized Attendance Modification", "AUTHORIZATION",
                "Detects attendance alteration by students or unauthorized faculty not assigned to target subject.",
                1, 1, 30, "HIGH");

        upsertRuleConfig("RULE-MOD-002", "Bulk Academic Record Modification", "INTEGRITY",
                "Monitors velocity of grade or attendance modifications exceeding configurable threshold in a sliding window.",
                1, 3, 60, "CRITICAL");

        upsertRuleConfig("RULE-PRV-003", "Unauthorized Privilege Changes", "ACCESS_CONTROL",
                "Detects unauthorized role manipulation, privilege elevation, or account status mutation by non-admins.",
                1, 1, 30, "CRITICAL");

        upsertRuleConfig("RULE-REC-004", "Unauthorized Student Record Access", "ACCESS_CONTROL",
                "Detects access to student academic records outside of ownership boundary or faculty course assignment.",
                1, 1, 30, "HIGH");

        upsertRuleConfig("RULE-TIM-005", "Off-Hours Administrative Activity", "ANOMALY",
                "Detects high-impact administrative or gradebook operations conducted between 02:00 AM and 05:00 AM.",
                1, 2, 120, "MEDIUM");

        upsertRuleConfig("RULE-BRT-006", "Brute Force Credential Abuse", "AUTHENTICATION",
                "Flags repeated failed authentication attempts against campus accounts from the same source.",
                1, 3, 60, "HIGH");
    }

    private void upsertRuleConfig(String ruleCode, String ruleName, String category, String description,
                                  int enabled, int threshold, int windowSeconds, String severity) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM detection_rule_configs WHERE rule_code = ?", Integer.class, ruleCode);
        if (count == null || count == 0) {
            String sql = """
                INSERT INTO detection_rule_configs (rule_code, rule_name, category, description, enabled, threshold, time_window_seconds, severity, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
            """;
            jdbcTemplate.update(sql, ruleCode, ruleName, category, description, enabled, threshold, windowSeconds, severity);
        }
    }

    private void upsertSubject(String code, String name, String dept, int credits, int semester, String desc) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subjects WHERE UPPER(code) = UPPER(?)", Integer.class, code);
        if (count == null || count == 0) {
            String sql = "INSERT INTO subjects (code, name, department, credits, semester, description) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, code, name, dept, credits, semester, desc);
        }
    }

    private void assignFaculty(Long facultyId, Long subjectId, String semester, String year) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM faculty_subjects WHERE faculty_id = ? AND subject_id = ? AND assigned_semester = ?",
                Integer.class, facultyId, subjectId, semester);
        if (count == null || count == 0) {
            String sql = "INSERT INTO faculty_subjects (faculty_id, subject_id, assigned_semester, academic_year) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, facultyId, subjectId, semester, year);
        }
    }

    private void enrollStudent(Long studentId, Long subjectId, String semester) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND subject_id = ? AND semester = ?",
                Integer.class, studentId, subjectId, semester);
        if (count == null || count == 0) {
            String sql = "INSERT INTO enrollments (student_id, subject_id, semester, enrollment_status, enrolled_at) VALUES (?, ?, ?, 'ENROLLED', datetime('now'))";
            jdbcTemplate.update(sql, studentId, subjectId, semester);
        }
    }

    private Long getUserIdByUsername(String username) {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1", Long.class, username);
        } catch (Exception e) {
            return null;
        }
    }

    private Long getSubjectIdByCode(String code) {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM subjects WHERE UPPER(code) = UPPER(?) LIMIT 1", Long.class, code);
        } catch (Exception e) {
            return null;
        }
    }
}

