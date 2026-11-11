package com.netsentinel.web.model;

import java.time.LocalDateTime;

public class Mark {
    private Long id;
    private Long studentId;
    private Long subjectId;
    private Long facultyId;
    private String examType; // MIDTERM, FINAL, LAB, ASSIGNMENT, QUIZ
    private Double score;
    private Double maxScore;
    private LocalDateTime recordedAt;

    // Presentation joins
    private String studentName;
    private String studentUserCode;
    private String subjectCode;
    private String subjectName;
    private String facultyName;

    public Mark() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentUserCode() { return studentUserCode; }
    public void setStudentUserCode(String studentUserCode) { this.studentUserCode = studentUserCode; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public double getPercentage() {
        if (maxScore == null || maxScore <= 0 || score == null) return 0.0;
        return (score / maxScore) * 100.0;
    }
}

