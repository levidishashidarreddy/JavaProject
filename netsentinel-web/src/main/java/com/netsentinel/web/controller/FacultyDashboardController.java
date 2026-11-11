package com.netsentinel.web.controller;

import com.netsentinel.web.model.FacultySubject;
import com.netsentinel.web.security.UserSession;
import com.netsentinel.web.service.AcademicService;
import com.netsentinel.web.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/faculty")
public class FacultyDashboardController {

    private final AcademicService academicService;
    private final ActivityLogService activityLogService;

    public FacultyDashboardController(AcademicService academicService, ActivityLogService activityLogService) {
        this.academicService = academicService;
        this.activityLogService = activityLogService;
    }

    private UserSession getUser(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model, HttpServletRequest request) {
        UserSession user = getUser(session);
        List<FacultySubject> assignedSubjects = academicService.getFacultySubjects(user.getId());
        model.addAttribute("assignedSubjects", assignedSubjects);
        model.addAttribute("recentLogs", activityLogService.getRecentLogsByActor(user.getUsername(), 6));

        activityLogService.logAllowed(user.getId(), user.getUsername(), user.getRole().name(),
                "VIEW_FACULTY_DASHBOARD", "DASHBOARD", String.valueOf(user.getId()), "/faculty/dashboard", request.getRemoteAddr(),
                "Faculty accessed portal dashboard");

        return "faculty/dashboard";
    }

    @GetMapping("/subjects")
    public String mySubjects(HttpSession session, Model model) {
        UserSession user = getUser(session);
        model.addAttribute("assignedSubjects", academicService.getFacultySubjects(user.getId()));
        return "faculty/subjects";
    }

    @GetMapping("/students")
    public String myStudents(HttpSession session, Model model, @RequestParam(value = "subjectId", required = false) Long subjectId) {
        UserSession user = getUser(session);
        List<FacultySubject> subjects = academicService.getFacultySubjects(user.getId());
        model.addAttribute("assignedSubjects", subjects);

        Long selectedSubId = subjectId;
        if (selectedSubId == null && !subjects.isEmpty()) {
            selectedSubId = subjects.get(0).getSubjectId();
        }
        model.addAttribute("selectedSubjectId", selectedSubId);

        if (selectedSubId != null) {
            model.addAttribute("enrolledStudents", academicService.getSubjectStudents(selectedSubId));
        }

        return "faculty/students";
    }

    @GetMapping("/attendance")
    public String attendanceManagement(HttpSession session, Model model, @RequestParam(value = "subjectId", required = false) Long subjectId) {
        UserSession user = getUser(session);
        List<FacultySubject> subjects = academicService.getFacultySubjects(user.getId());
        model.addAttribute("assignedSubjects", subjects);

        Long selectedSubId = subjectId;
        if (selectedSubId == null && !subjects.isEmpty()) {
            selectedSubId = subjects.get(0).getSubjectId();
        }
        model.addAttribute("selectedSubjectId", selectedSubId);

        if (selectedSubId != null) {
            model.addAttribute("enrolledStudents", academicService.getSubjectStudents(selectedSubId));
        }

        return "faculty/attendance";
    }

    @PostMapping("/attendance/record")
    public String recordAttendance(@RequestParam("studentId") Long studentId,
                                   @RequestParam("subjectId") Long subjectId,
                                   @RequestParam("status") String status,
                                   @RequestParam(value = "remarks", required = false) String remarks,
                                   @RequestParam(value = "date", required = false) String dateStr,
                                   HttpSession session,
                                   HttpServletRequest request) {
        UserSession user = getUser(session);
        LocalDate date = (dateStr != null && !dateStr.isBlank()) ? LocalDate.parse(dateStr) : LocalDate.now();

        // Enforces RULE 1
        boolean success = academicService.recordAttendance(user.getId(), user.getUsername(), studentId, subjectId, date, status, remarks, request.getRemoteAddr());
        return "redirect:/faculty/attendance?subjectId=" + subjectId + (success ? "&success=recorded" : "&error=unauthorized");
    }

    @GetMapping("/marks")
    public String marksManagement(HttpSession session, Model model, @RequestParam(value = "subjectId", required = false) Long subjectId) {
        UserSession user = getUser(session);
        List<FacultySubject> subjects = academicService.getFacultySubjects(user.getId());
        model.addAttribute("assignedSubjects", subjects);

        Long selectedSubId = subjectId;
        if (selectedSubId == null && !subjects.isEmpty()) {
            selectedSubId = subjects.get(0).getSubjectId();
        }
        model.addAttribute("selectedSubjectId", selectedSubId);

        if (selectedSubId != null) {
            model.addAttribute("enrolledStudents", academicService.getSubjectStudents(selectedSubId));
        }

        return "faculty/marks";
    }

    @PostMapping("/marks/record")
    public String recordMark(@RequestParam("studentId") Long studentId,
                             @RequestParam("subjectId") Long subjectId,
                             @RequestParam("examType") String examType,
                             @RequestParam("score") Double score,
                             @RequestParam(value = "maxScore", defaultValue = "100.0") Double maxScore,
                             HttpSession session,
                             HttpServletRequest request) {
        UserSession user = getUser(session);
        // Enforces RULE 1 and monitors velocity for RULE 2
        boolean success = academicService.recordMark(user.getId(), user.getUsername(), studentId, subjectId, examType, score, maxScore, request.getRemoteAddr());
        return "redirect:/faculty/marks?subjectId=" + subjectId + (success ? "&success=mark_recorded" : "&error=unauthorized");
    }

    @GetMapping("/activity")
    public String myActivity(HttpSession session, Model model) {
        UserSession user = getUser(session);
        model.addAttribute("activities", activityLogService.getRecentLogsByActor(user.getUsername(), 25));
        return "faculty/activity";
    }
}
