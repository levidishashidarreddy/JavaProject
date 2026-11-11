-- NetSentinel SQLite Database Schema
-- Network Anomaly Detection and Incident Analyzer

PRAGMA foreign_keys = ON;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_code VARCHAR(32) NOT NULL UNIQUE,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL CHECK (role IN ('STUDENT', 'FACULTY', 'ADMIN')),
    department VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- 2. Subjects Table
CREATE TABLE IF NOT EXISTS subjects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    department VARCHAR(64) NOT NULL,
    credits INTEGER NOT NULL DEFAULT 4,
    semester INTEGER NOT NULL DEFAULT 5,
    description TEXT
);

CREATE INDEX IF NOT EXISTS idx_subjects_code ON subjects(code);

-- 3. Faculty-Subject Assignments
CREATE TABLE IF NOT EXISTS faculty_subjects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    faculty_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,
    assigned_semester VARCHAR(32) NOT NULL DEFAULT 'Fall 2026',
    academic_year VARCHAR(32) NOT NULL DEFAULT '2026-2027',
    FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE (faculty_id, subject_id, assigned_semester, academic_year)
);

CREATE INDEX IF NOT EXISTS idx_faculty_sub ON faculty_subjects(faculty_id, subject_id);

-- 4. Enrollments Table
CREATE TABLE IF NOT EXISTS enrollments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,
    semester VARCHAR(32) NOT NULL DEFAULT 'Fall 2026',
    enrollment_status VARCHAR(32) NOT NULL DEFAULT 'ENROLLED' CHECK (enrollment_status IN ('ENROLLED', 'DROPPED', 'COMPLETED')),
    enrolled_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE (student_id, subject_id, semester)
);

CREATE INDEX IF NOT EXISTS idx_enrollments_student ON enrollments(student_id);

-- 5. Attendance Table
CREATE TABLE IF NOT EXISTS attendance (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,
    faculty_id INTEGER NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),
    remarks VARCHAR(255),
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_attendance_stu_sub ON attendance(student_id, subject_id);
CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(date);

-- 6. Marks Table
CREATE TABLE IF NOT EXISTS marks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,
    faculty_id INTEGER NOT NULL,
    exam_type VARCHAR(64) NOT NULL CHECK (exam_type IN ('MIDTERM', 'FINAL', 'LAB', 'ASSIGNMENT', 'QUIZ')),
    score REAL NOT NULL,
    max_score REAL NOT NULL DEFAULT 100.0,
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_marks_stu_sub ON marks(student_id, subject_id);

-- 7. Activity Logs Table (Audit trail)
CREATE TABLE IF NOT EXISTS activity_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    actor_id INTEGER,
    actor_username VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_resource VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) DEFAULT 'RESOURCE',
    target_id VARCHAR(64),
    request_id VARCHAR(64),
    outcome VARCHAR(32) NOT NULL CHECK (outcome IN ('SUCCESS', 'DENIED', 'BLOCKED', 'FLAGGED', 'ALLOWED')),
    ip_address VARCHAR(45) NOT NULL DEFAULT '127.0.0.1',
    user_agent VARCHAR(255),
    details TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_activity_timestamp ON activity_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_activity_actor ON activity_logs(actor_username);
CREATE INDEX IF NOT EXISTS idx_activity_action ON activity_logs(action);
CREATE INDEX IF NOT EXISTS idx_activity_target ON activity_logs(target_type, target_id);

-- 8. Incidents Table (Security Anomaly Detection)
CREATE TABLE IF NOT EXISTS incidents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    incident_number VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE')),
    actor_id INTEGER,
    actor_username VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    target_resource VARCHAR(128) NOT NULL,
    rule_code VARCHAR(32) NOT NULL,
    confidence_score INTEGER NOT NULL DEFAULT 85,
    reason TEXT NOT NULL,
    evidence_json TEXT,
    insight_explanation TEXT,
    recommended_action TEXT,
    assigned_to VARCHAR(64) DEFAULT 'Admin Dave',
    reviewed_by VARCHAR(64),
    reviewed_at DATETIME,
    investigation_notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_incidents_severity ON incidents(severity);
CREATE INDEX IF NOT EXISTS idx_incidents_status ON incidents(status);
CREATE INDEX IF NOT EXISTS idx_incidents_created ON incidents(created_at);

-- 9. Incident Events Table (Timeline & Audit within an incident)
CREATE TABLE IF NOT EXISTS incident_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    incident_id INTEGER NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_id INTEGER,
    actor_username VARCHAR(64) NOT NULL,
    notes TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_incident_events_inc ON incident_events(incident_id);

-- 10. Detection Rule Configs Table
CREATE TABLE IF NOT EXISTS detection_rule_configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    rule_code VARCHAR(32) NOT NULL UNIQUE,
    rule_name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT 1,
    threshold INTEGER NOT NULL DEFAULT 5,
    time_window_seconds INTEGER NOT NULL DEFAULT 60,
    severity VARCHAR(32) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 11. Simulation Runs Table
CREATE TABLE IF NOT EXISTS simulation_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    simulation_type VARCHAR(64) NOT NULL,
    trigger_payload TEXT NOT NULL,
    detected BOOLEAN NOT NULL DEFAULT 1,
    incident_id INTEGER,
    run_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    summary TEXT,
    FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE SET NULL
);

-- 12. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    recipient_role VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    title VARCHAR(128) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'INFO',
    read_status BOOLEAN NOT NULL DEFAULT 0,
    link VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_role ON notifications(recipient_role, read_status);
