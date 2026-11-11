package com.netsentinel.web.model;

public class FacultySubject {
    private Long id;
    private Long facultyId;
    private Long subjectId;
    private String assignedSemester;
    private String academicYear;

    // Presentation joins
    private String facultyName;
    private String facultyUserCode;
    private String subjectCode;
    private String subjectName;
    private Integer credits;

    public FacultySubject() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public String getAssignedSemester() { return assignedSemester; }
    public void setAssignedSemester(String assignedSemester) { this.assignedSemester = assignedSemester; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public String getFacultyUserCode() { return facultyUserCode; }
    public void setFacultyUserCode(String facultyUserCode) { this.facultyUserCode = facultyUserCode; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
}

