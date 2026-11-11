package com.netsentinel.web.controller;

import com.netsentinel.web.model.Subject;
import com.netsentinel.web.model.UserRole;
import com.netsentinel.web.repository.AcademicRepository;
import com.netsentinel.web.repository.SubjectRepository;
import com.netsentinel.web.repository.UserRepository;
import com.netsentinel.web.security.UserSession;
import com.netsentinel.web.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/academics")
public class AcademicManagementController {

    private final SubjectRepository subjectRepository;
    private final AcademicRepository academicRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AcademicManagementController(SubjectRepository subjectRepository,
                                        AcademicRepository academicRepository,
                                        UserRepository userRepository,
                                        AuditLogService auditLogService) {
        this.subjectRepository = subjectRepository;
        this.academicRepository = academicRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String viewAcademics(Model model) {
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("facultyAssignments", subjectRepository.findFacultyAssignments());
        model.addAttribute("enrollments", academicRepository.findAllEnrollments());
        model.addAttribute("facultyList", userRepository.findByRole(UserRole.FACULTY));
        model.addAttribute("studentList", userRepository.findByRole(UserRole.STUDENT));
        model.addAttribute("totalAttendanceCount", academicRepository.countTotalAttendance());
        model.addAttribute("totalMarksCount", academicRepository.countTotalMarks());
        return "admin/academics";
    }

    @PostMapping("/subjects/add")
    public String addSubject(@RequestParam("code") String code,
                             @RequestParam("name") String name,
                             @RequestParam("department") String department,
                             @RequestParam(value = "credits", defaultValue = "4") Integer credits,
                             @RequestParam(value = "semester", defaultValue = "5") Integer semester,
                             @RequestParam(value = "description", required = false) String description,
                             HttpSession session,
                             HttpServletRequest request) {
        UserSession admin = (UserSession) session.getAttribute("userSession");

        Subject s = new Subject(null, code.trim().toUpperCase(), name.trim(), department.trim(), credits, semester, description);
        try {
            subjectRepository.insert(s);
            auditLogService.log(admin.getId(), admin.getUsername(), "ADMIN", "SUBJECT_CREATE", "/admin/academics/subjects/add",
                    "SUCCESS", request.getRemoteAddr(), request.getHeader("User-Agent"), "Created subject curriculum " + code);
            return "redirect:/admin/academics?success=subject_added";
        } catch (Exception e) {
            return "redirect:/admin/academics?error=duplicate_subject";
        }
    }

    @PostMapping("/assign-faculty")
    public String assignFaculty(@RequestParam("facultyId") Long facultyId,
                                @RequestParam("subjectId") Long subjectId,
                                @RequestParam(value = "semester", defaultValue = "Fall 2026") String semester,
                                @RequestParam(value = "academicYear", defaultValue = "2026-2027") String academicYear,
                                HttpSession session,
                                HttpServletRequest request) {
        UserSession admin = (UserSession) session.getAttribute("userSession");
        subjectRepository.assignFaculty(facultyId, subjectId, semester, academicYear);

        auditLogService.log(admin.getId(), admin.getUsername(), "ADMIN", "FACULTY_ASSIGN", "/admin/academics/assign-faculty",
                "SUCCESS", request.getRemoteAddr(), request.getHeader("User-Agent"),
                "Assigned faculty ID " + facultyId + " to subject ID " + subjectId);

        return "redirect:/admin/academics?success=assigned";
    }

    @PostMapping("/enroll-student")
    public String enrollStudent(@RequestParam("studentId") Long studentId,
                                @RequestParam("subjectId") Long subjectId,
                                @RequestParam(value = "semester", defaultValue = "Fall 2026") String semester,
                                HttpSession session,
                                HttpServletRequest request) {
        UserSession admin = (UserSession) session.getAttribute("userSession");
        academicRepository.enrollStudent(studentId, subjectId, semester);

        auditLogService.log(admin.getId(), admin.getUsername(), "ADMIN", "STUDENT_ENROLL", "/admin/academics/enroll-student",
                "SUCCESS", request.getRemoteAddr(), request.getHeader("User-Agent"),
                "Enrolled student ID " + studentId + " in subject ID " + subjectId);

        return "redirect:/admin/academics?success=enrolled";
    }
}

