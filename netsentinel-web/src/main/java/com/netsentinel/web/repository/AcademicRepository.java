package com.netsentinel.web.repository;

import com.netsentinel.web.model.Attendance;
import com.netsentinel.web.model.Enrollment;
import com.netsentinel.web.model.Mark;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AcademicRepository {

    private final JdbcTemplate jdbcTemplate;

    public AcademicRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Enrollment> enrollmentRowMapper = (rs, rowNum) -> {
        Enrollment e = new Enrollment();
        e.setId(rs.getLong("id"));
        e.setStudentId(rs.getLong("student_id"));
        e.setSubjectId(rs.getLong("subject_id"));
        e.setSemester(rs.getString("semester"));
        e.setEnrollmentStatus(rs.getString("enrollment_status"));
        
        String dt = rs.getString("enrolled_at");
        if (dt != null) {
            try {
                if (dt.length() > 19) dt = dt.substring(0, 19);
                e.setEnrolledAt(LocalDateTime.parse(dt.replace(" ", "T")));
            } catch (Exception ignored) {
                e.setEnrolledAt(LocalDateTime.now());
            }
        }
        e.setStudentName(rs.getString("student_name"));
        e.setStudentUserCode(rs.getString("student_user_code"));
        e.setSubjectCode(rs.getString("subject_code"));
        e.setSubjectName(rs.getString("subject_name"));
        e.setCredits(rs.getInt("credits"));
        return e;
    };

    private final RowMapper<Attendance> attendanceRowMapper = (rs, rowNum) -> {
        Attendance a = new Attendance();
        a.setId(rs.getLong("id"));
        a.setStudentId(rs.getLong("student_id"));
        a.setSubjectId(rs.getLong("subject_id"));
        a.setFacultyId(rs.getLong("faculty_id"));
        
        String dStr = rs.getString("date");
        if (dStr != null) {
            try {
                a.setDate(LocalDate.parse(dStr.trim()));
            } catch (Exception ignored) {
                a.setDate(LocalDate.now());
            }
        }
        a.setStatus(rs.getString("status"));
        a.setRemarks(rs.getString("remarks"));
        
        String dt = rs.getString("recorded_at");
        if (dt != null) {
            try {
                if (dt.length() > 19) dt = dt.substring(0, 19);
                a.setRecordedAt(LocalDateTime.parse(dt.replace(" ", "T")));
            } catch (Exception ignored) {
                a.setRecordedAt(LocalDateTime.now());
            }
        }
        a.setStudentName(rs.getString("student_name"));
        a.setStudentUserCode(rs.getString("student_user_code"));
        a.setSubjectCode(rs.getString("subject_code"));
        a.setSubjectName(rs.getString("subject_name"));
        a.setFacultyName(rs.getString("faculty_name"));
        return a;
    };

    private final RowMapper<Mark> markRowMapper = (rs, rowNum) -> {
        Mark m = new Mark();
        m.setId(rs.getLong("id"));
        m.setStudentId(rs.getLong("student_id"));
        m.setSubjectId(rs.getLong("subject_id"));
        m.setFacultyId(rs.getLong("faculty_id"));
        m.setExamType(rs.getString("exam_type"));
        m.setScore(rs.getDouble("score"));
        m.setMaxScore(rs.getDouble("max_score"));
        
        String dt = rs.getString("recorded_at");
        if (dt != null) {
            try {
                if (dt.length() > 19) dt = dt.substring(0, 19);
                m.setRecordedAt(LocalDateTime.parse(dt.replace(" ", "T")));
            } catch (Exception ignored) {
                m.setRecordedAt(LocalDateTime.now());
            }
        }
        m.setStudentName(rs.getString("student_name"));
        m.setStudentUserCode(rs.getString("student_user_code"));
        m.setSubjectCode(rs.getString("subject_code"));
        m.setSubjectName(rs.getString("subject_name"));
        m.setFacultyName(rs.getString("faculty_name"));
        return m;
    };

    // Enrollments
    public List<Enrollment> findEnrollmentsByStudent(Long studentId) {
        String sql = """
            SELECT e.id, e.student_id, e.subject_id, e.semester, e.enrollment_status, e.enrolled_at,
                   u.full_name AS student_name, u.user_code AS student_user_code,
                   s.code AS subject_code, s.name AS subject_name, s.credits
            FROM enrollments e
            JOIN users u ON e.student_id = u.id
            JOIN subjects s ON e.subject_id = s.id
            WHERE e.student_id = ?
            ORDER BY s.code ASC
        """;
        return jdbcTemplate.query(sql, enrollmentRowMapper, studentId);
    }

    public List<Enrollment> findEnrollmentsBySubject(Long subjectId) {
        String sql = """
            SELECT e.id, e.student_id, e.subject_id, e.semester, e.enrollment_status, e.enrolled_at,
                   u.full_name AS student_name, u.user_code AS student_user_code,
                   s.code AS subject_code, s.name AS subject_name, s.credits
            FROM enrollments e
            JOIN users u ON e.student_id = u.id
            JOIN subjects s ON e.subject_id = s.id
            WHERE e.subject_id = ?
            ORDER BY u.full_name ASC
        """;
        return jdbcTemplate.query(sql, enrollmentRowMapper, subjectId);
    }

    public List<Enrollment> findAllEnrollments() {
        String sql = """
            SELECT e.id, e.student_id, e.subject_id, e.semester, e.enrollment_status, e.enrolled_at,
                   u.full_name AS student_name, u.user_code AS student_user_code,
                   s.code AS subject_code, s.name AS subject_name, s.credits
            FROM enrollments e
            JOIN users u ON e.student_id = u.id
            JOIN subjects s ON e.subject_id = s.id
            ORDER BY s.code ASC, u.full_name ASC
        """;
        return jdbcTemplate.query(sql, enrollmentRowMapper);
    }

    public void enrollStudent(Long studentId, Long subjectId, String semester) {
        String sql = "INSERT OR IGNORE INTO enrollments (student_id, subject_id, semester, enrollment_status) VALUES (?, ?, ?, 'ENROLLED')";
        jdbcTemplate.update(sql, studentId, subjectId, semester);
    }

    public boolean isStudentEnrolledInSubject(Long studentId, Long subjectId) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND subject_id = ? AND enrollment_status = 'ENROLLED'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studentId, subjectId);
        return count != null && count > 0;
    }

    public boolean isStudentAssignedToFaculty(Long studentId, Long facultyId) {
        String sql = """
            SELECT COUNT(*) FROM enrollments e
            JOIN faculty_subjects fs ON e.subject_id = fs.subject_id
            WHERE e.student_id = ? AND fs.faculty_id = ? AND e.enrollment_status = 'ENROLLED'
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studentId, facultyId);
        return count != null && count > 0;
    }

    public java.util.Set<Long> getFacultyAssignedStudentIds(Long facultyId) {
        String sql = """
            SELECT DISTINCT e.student_id FROM enrollments e
            JOIN faculty_subjects fs ON e.subject_id = fs.subject_id
            WHERE fs.faculty_id = ? AND e.enrollment_status = 'ENROLLED'
        """;
        List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, facultyId);
        return new java.util.HashSet<>(ids);
    }

    // Attendance
    public List<Attendance> findAttendanceByStudent(Long studentId) {
        String sql = """
            SELECT a.id, a.student_id, a.subject_id, a.faculty_id, a.date, a.status, a.remarks, a.recorded_at,
                   u.full_name AS student_name, u.user_code AS student_user_code,
                   s.code AS subject_code, s.name AS subject_name,
                   f.full_name AS faculty_name
            FROM attendance a
            JOIN users u ON a.student_id = u.id
            JOIN subjects s ON a.subject_id = s.id
            JOIN users f ON a.faculty_id = f.id
            WHERE a.student_id = ?
            ORDER BY a.date DESC
        """;
        return jdbcTemplate.query(sql, attendanceRowMapper, studentId);
    }

    public List<Attendance> findAttendanceBySubject(Long subjectId) {
        String sql = """
            SELECT a.id, a.student_id, a.subject_id, a.faculty_id, a.date, a.status, a.remarks, a.recorded_at,
                   u.full_name AS student_name, u.user_code AS student_user_code,
                   s.code AS subject_code, s.name AS subject_name,
                   f.full_name AS faculty_name
            FROM attendance a
            JOIN users u ON a.student_id = u.id
            JOIN subjects s ON a.subject_id = s.id
            JOIN users f ON a.faculty_id = f.id
            WHERE a.subject_id = ?
            ORDER BY a.date DESC
        """;
        return jdbcTemplate.query(sql, attendanceRowMapper, subjectId);
    }

    public Long recordAttendance(Attendance attendance) {
        String sql = """
            INSERT INTO attendance (student_id, subject_id, faculty_id, date, status, remarks, recorded_at)
            VALUES (?, ?, ?, ?, ?, ?, datetime('now'))
        """;
        jdbcTemplate.update(sql, attendance.getStudentId(), attendance.getSubjectId(), attendance.getFacultyId(),
                attendance.getDate().toString(), attendance.getStatus(), attendance.getRemarks());
        return 1L;
    }

    // Marks
    public List<Mark> findMarksByStudent(Long studentId) {
        String sql = """
            SELECT m.id, m.student_id, m.subject_id, m.faculty_id, m.exam_type, m.score, m.max_score, m.recorded_at,
                   u.full_name AS student_name, u.user_code AS student_user_code,
                   s.code AS subject_code, s.name AS subject_name,
                   f.full_name AS faculty_name
            FROM marks m
            JOIN users u ON m.student_id = u.id
            JOIN subjects s ON m.subject_id = s.id
            JOIN users f ON m.faculty_id = f.id
            WHERE m.student_id = ?
            ORDER BY s.code ASC, m.recorded_at DESC
        """;
        return jdbcTemplate.query(sql, markRowMapper, studentId);
    }

    public List<Mark> findMarksBySubject(Long subjectId) {
        String sql = """
            SELECT m.id, m.student_id, m.subject_id, m.faculty_id, m.exam_type, m.score, m.max_score, m.recorded_at,
                   u.full_name AS student_name, u.user_code AS student_user_code,
                   s.code AS subject_code, s.name AS subject_name,
                   f.full_name AS faculty_name
            FROM marks m
            JOIN users u ON m.student_id = u.id
            JOIN subjects s ON m.subject_id = s.id
            JOIN users f ON m.faculty_id = f.id
            WHERE m.subject_id = ?
            ORDER BY u.full_name ASC, m.recorded_at DESC
        """;
        return jdbcTemplate.query(sql, markRowMapper, subjectId);
    }

    public Long recordMark(Mark mark) {
        String sql = """
            INSERT INTO marks (student_id, subject_id, faculty_id, exam_type, score, max_score, recorded_at)
            VALUES (?, ?, ?, ?, ?, ?, datetime('now'))
        """;
        jdbcTemplate.update(sql, mark.getStudentId(), mark.getSubjectId(), mark.getFacultyId(),
                mark.getExamType(), mark.getScore(), mark.getMaxScore());
        return 1L;
    }

    public Map<String, Object> getStudentAttendanceSummary(Long studentId) {
        String sql = """
            SELECT 
                COUNT(*) as total_classes,
                SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) as present_count,
                SUM(CASE WHEN status = 'LATE' THEN 1 ELSE 0 END) as late_count,
                SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) as absent_count
            FROM attendance
            WHERE student_id = ?
        """;
        return jdbcTemplate.queryForMap(sql, studentId);
    }

    public int countTotalAttendance() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM attendance", Integer.class);
        return count != null ? count : 0;
    }

    public int countTotalMarks() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM marks", Integer.class);
        return count != null ? count : 0;
    }
}
