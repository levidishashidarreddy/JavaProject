package com.netsentinel.web.repository;

import com.netsentinel.web.model.FacultySubject;
import com.netsentinel.web.model.Subject;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SubjectRepository {

    private final JdbcTemplate jdbcTemplate;

    public SubjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Subject> subjectRowMapper = (rs, rowNum) -> new Subject(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("department"),
            rs.getInt("credits"),
            rs.getInt("semester"),
            rs.getString("description")
    );

    private final RowMapper<FacultySubject> facultySubjectRowMapper = (rs, rowNum) -> {
        FacultySubject fs = new FacultySubject();
        fs.setId(rs.getLong("id"));
        fs.setFacultyId(rs.getLong("faculty_id"));
        fs.setSubjectId(rs.getLong("subject_id"));
        fs.setAssignedSemester(rs.getString("assigned_semester"));
        fs.setAcademicYear(rs.getString("academic_year"));
        fs.setFacultyName(rs.getString("faculty_name"));
        fs.setFacultyUserCode(rs.getString("faculty_user_code"));
        fs.setSubjectCode(rs.getString("subject_code"));
        fs.setSubjectName(rs.getString("subject_name"));
        fs.setCredits(rs.getInt("credits"));
        return fs;
    };

    public List<Subject> findAll() {
        return jdbcTemplate.query("SELECT * FROM subjects ORDER BY code ASC", subjectRowMapper);
    }

    public Optional<Subject> findById(Long id) {
        String sql = "SELECT * FROM subjects WHERE id = ?";
        try {
            Subject sub = jdbcTemplate.queryForObject(sql, subjectRowMapper, id);
            return Optional.ofNullable(sub);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Subject> findByCode(String code) {
        String sql = "SELECT * FROM subjects WHERE UPPER(code) = UPPER(?) LIMIT 1";
        try {
            Subject sub = jdbcTemplate.queryForObject(sql, subjectRowMapper, code);
            return Optional.ofNullable(sub);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Long insert(Subject subject) {
        String sql = "INSERT INTO subjects (code, name, department, credits, semester, description) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, subject.getCode(), subject.getName(), subject.getDepartment(), 
                            subject.getCredits(), subject.getSemester(), subject.getDescription());
        return jdbcTemplate.queryForObject("SELECT id FROM subjects WHERE UPPER(code) = UPPER(?)", Long.class, subject.getCode());
    }

    public List<FacultySubject> findFacultyAssignments() {
        String sql = """
            SELECT fs.id, fs.faculty_id, fs.subject_id, fs.assigned_semester, fs.academic_year,
                   u.full_name AS faculty_name, u.user_code AS faculty_user_code,
                   s.code AS subject_code, s.name AS subject_name, s.credits
            FROM faculty_subjects fs
            JOIN users u ON fs.faculty_id = u.id
            JOIN subjects s ON fs.subject_id = s.id
            ORDER BY s.code ASC, u.full_name ASC
        """;
        return jdbcTemplate.query(sql, facultySubjectRowMapper);
    }

    public List<FacultySubject> findSubjectsByFaculty(Long facultyId) {
        String sql = """
            SELECT fs.id, fs.faculty_id, fs.subject_id, fs.assigned_semester, fs.academic_year,
                   u.full_name AS faculty_name, u.user_code AS faculty_user_code,
                   s.code AS subject_code, s.name AS subject_name, s.credits
            FROM faculty_subjects fs
            JOIN users u ON fs.faculty_id = u.id
            JOIN subjects s ON fs.subject_id = s.id
            WHERE fs.faculty_id = ?
            ORDER BY s.code ASC
        """;
        return jdbcTemplate.query(sql, facultySubjectRowMapper, facultyId);
    }

    public boolean isFacultyAssigned(Long facultyId, Long subjectId) {
        String sql = "SELECT COUNT(*) FROM faculty_subjects WHERE faculty_id = ? AND subject_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, facultyId, subjectId);
        return count != null && count > 0;
    }

    public void assignFaculty(Long facultyId, Long subjectId, String semester, String year) {
        String sql = "INSERT OR IGNORE INTO faculty_subjects (faculty_id, subject_id, assigned_semester, academic_year) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, facultyId, subjectId, semester, year);
    }

    public int countTotal() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subjects", Integer.class);
        return count != null ? count : 0;
    }
}
