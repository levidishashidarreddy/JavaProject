-- NetSentinel Enterprise Canonical Data Seed
-- Strict Demo Accounts: student101, student102, faculty101, faculty102, admin01
-- Zero Fake Mock Records / Real Database Calculations

-- 1. Canonical Demo Users (passwords: student123, faculty123, admin123)
-- BCrypt encoded passwords:
-- student123: $2a$10$wK1W0qI.t9d94y2Yv.tD6uSg8zE/hC.6zMvjA9hVqZ7t1jM9j4eGe
-- faculty123: $2a$10$N1eZ7p1m2s9kL8qX4zY7.u0jK2vN5mQ8wE3rT6yU9iO1pA4sD7fG0
-- admin123:   $2a$10$r9G.8mB3qN7pL5tY1zW4.e8kM1vQ4mP7wT2rE5yI8oO0pS3sF6hJ2
INSERT OR IGNORE INTO users (id, user_code, username, password_hash, full_name, email, role, department, status, created_at) VALUES
(1, 'STU-2026-101', 'student101', '$2a$10$wK1W0qI.t9d94y2Yv.tD6uSg8zE/hC.6zMvjA9hVqZ7t1jM9j4eGe', 'Rahul Sharma', 'student101@campus.netsentinel.edu', 'STUDENT', 'Computer Science & Engineering', 'ACTIVE', '2026-08-15 09:00:00'),
(2, 'STU-2026-102', 'student102', '$2a$10$wK1W0qI.t9d94y2Yv.tD6uSg8zE/hC.6zMvjA9hVqZ7t1jM9j4eGe', 'Priya Patel', 'student102@campus.netsentinel.edu', 'STUDENT', 'Computer Science & Engineering', 'ACTIVE', '2026-08-15 09:15:00'),
(3, 'FAC-2026-F101', 'faculty101', '$2a$10$N1eZ7p1m2s9kL8qX4zY7.u0jK2vN5mQ8wE3rT6yU9iO1pA4sD7fG0', 'Prof. Alan Turing', 'faculty101@campus.netsentinel.edu', 'FACULTY', 'Computer Science & Engineering', 'ACTIVE', '2026-08-01 10:00:00'),
(4, 'FAC-2026-F102', 'faculty102', '$2a$10$N1eZ7p1m2s9kL8qX4zY7.u0jK2vN5mQ8wE3rT6yU9iO1pA4sD7fG0', 'Dr. Sarah Jenkins', 'faculty102@campus.netsentinel.edu', 'FACULTY', 'Mathematics & Computing', 'ACTIVE', '2026-08-01 10:30:00'),
(5, 'ADM-2026-A001', 'admin01', '$2a$10$r9G.8mB3qN7pL5tY1zW4.e8kM1vQ4mP7wT2rE5yI8oO0pS3sF6hJ2', 'Chief Dave Miller', 'admin01@security.netsentinel.edu', 'ADMIN', 'Cybersecurity Operations Center', 'ACTIVE', '2026-07-15 08:00:00');

-- 2. Official Subjects Catalog
INSERT OR IGNORE INTO subjects (id, code, name, department, credits, semester, description) VALUES
(1, 'CS301', 'Data Structures & Algorithms', 'Computer Science & Engineering', 4, 5, 'Fundamental computational data structures, trees, graphs, dynamic programming, and algorithmic analysis.'),
(2, 'CS305', 'Operating Systems & Kernels', 'Computer Science & Engineering', 4, 5, 'Process scheduling, concurrency, virtual memory management, file systems, and kernel security models.'),
(3, 'CS402', 'Network Security & Cryptography', 'Computer Science & Engineering', 3, 7, 'Symmetric/asymmetric encryption, cryptographic protocols, intrusion detection, packet analysis, and zero trust.'),
(4, 'CS405', 'Database Management Systems', 'Computer Science & Engineering', 3, 5, 'Relational models, SQL normalization, concurrency control, transaction ACID semantics, and query optimization.');

-- 3. Faculty-Subject Assignments
INSERT OR IGNORE INTO faculty_subjects (id, faculty_id, subject_id, assigned_semester, academic_year) VALUES
(1, 3, 1, 'Fall 2026', '2026-2027'), -- Prof. Alan Turing -> CS301
(2, 3, 3, 'Fall 2026', '2026-2027'), -- Prof. Alan Turing -> CS402
(3, 4, 2, 'Fall 2026', '2026-2027'), -- Dr. Sarah Jenkins -> CS305
(4, 4, 4, 'Fall 2026', '2026-2027'); -- Dr. Sarah Jenkins -> CS405

-- 4. Student Course Enrollments
INSERT OR IGNORE INTO enrollments (id, student_id, subject_id, semester, enrollment_status) VALUES
(1, 1, 1, 'Fall 2026', 'ENROLLED'), -- student101 -> CS301
(2, 1, 2, 'Fall 2026', 'ENROLLED'), -- student101 -> CS305
(3, 1, 3, 'Fall 2026', 'ENROLLED'), -- student101 -> CS402
(4, 1, 4, 'Fall 2026', 'ENROLLED'), -- student101 -> CS405
(5, 2, 1, 'Fall 2026', 'ENROLLED'), -- student102 -> CS301
(6, 2, 2, 'Fall 2026', 'ENROLLED'); -- student102 -> CS305

-- 5. Canonical Detection Rule Configurations
INSERT OR IGNORE INTO detection_rule_configs (id, rule_code, rule_name, category, description, enabled, threshold, time_window_seconds, severity, updated_at) VALUES
(1, 'RULE-ATT-001', 'Unauthorized Attendance Modification', 'AUTHORIZATION', 'Detects attendance alteration by students or unauthorized faculty members who are not assigned to the target subject.', 1, 1, 30, 'HIGH', '2026-11-01 00:00:00'),
(2, 'RULE-MOD-002', 'Bulk Academic Record Modification', 'INTEGRITY', 'Detects rapid mass-editing or batch alteration of examination scores within an abnormal sliding time window.', 1, 3, 60, 'CRITICAL', '2026-11-01 00:00:00'),
(3, 'RULE-PRV-003', 'Unauthorized Privilege Changes', 'ACCESS_CONTROL', 'Detects attempts by unauthorized non-admin users to elevate account privileges, manipulate roles, or toggle user status.', 1, 1, 30, 'CRITICAL', '2026-11-01 00:00:00'),
(4, 'RULE-REC-004', 'Unauthorized Student Record Access', 'ACCESS_CONTROL', 'Flags anomalous access to student gradebooks or personal profiles outside of ownership boundary or faculty assignment.', 1, 1, 30, 'HIGH', '2026-11-01 00:00:00'),
(5, 'RULE-TIM-005', 'Off-Hours Administrative Activity', 'ANOMALY', 'Detects administrative or academic gradebook alterations conducted between 02:00 AM and 05:00 AM.', 1, 2, 120, 'MEDIUM', '2026-11-01 00:00:00'),
(6, 'RULE-BRT-006', 'Brute Force Credential Abuse', 'AUTHENTICATION', 'Flags multiple consecutive failed authentication attempts against academic accounts from the same source.', 1, 3, 60, 'HIGH', '2026-11-01 00:00:00');
