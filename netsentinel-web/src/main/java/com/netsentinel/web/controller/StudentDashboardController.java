package com.netsentinel.web.controller;

import com.netsentinel.web.security.UserSession;
import com.netsentinel.web.service.AcademicService;
import com.netsentinel.web.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/student")
public class StudentDashboardController {

    private final AcademicService academicService;
    private final ActivityLogService activityLogService;

    public StudentDashboardController(AcademicService academicService, ActivityLogService activityLogService) {
        this.academicService = academicService;
        this.activityLogService = activityLogService;
    }

    private UserSession getUser(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model, HttpServletRequest request) {
        UserSession user = getUser(session);
        Map<String, Object> overview = academicService.getStudentAcademicOverview(user.getId());
        model.addAttribute("overview", overview);

        activityLogService.logAllowed(user.getId(), user.getUsername(), user.getRole().name(),
                "VIEW_STUDENT_DASHBOARD", "DASHBOARD", String.valueOf(user.getId()), "/student/dashboard", request.getRemoteAddr(),
                "Student accessed personal dashboard overview");

        return "student/dashboard";
    }

    @GetMapping("/attendance")
    public String myAttendance(@RequestParam(value = "studentId", required = false) Long requestedStudentId,
                               HttpSession session, Model model, HttpServletRequest request) {
        UserSession user = getUser(session);
        Long targetId = (requestedStudentId != null) ? requestedStudentId : user.getId();

        // Enforce RULE 4 security boundary
        model.addAttribute("attendanceList", academicService.getStudentAttendanceSecure(
                user.getId(), user.getUsername(), user.getRole().name(), targetId, request.getRemoteAddr()));
        model.addAttribute("summary", academicService.getStudentAcademicOverview(targetId));

        return "student/attendance";
    }

    @GetMapping("/marks")
    public String myMarks(@RequestParam(value = "studentId", required = false) Long requestedStudentId,
                          HttpSession session, Model model, HttpServletRequest request) {
        UserSession user = getUser(session);
        Long targetId = (requestedStudentId != null) ? requestedStudentId : user.getId();

        // Enforce RULE 4 security boundary
        model.addAttribute("marksList", academicService.getStudentMarksSecure(
                user.getId(), user.getUsername(), user.getRole().name(), targetId, request.getRemoteAddr()));
        model.addAttribute("summary", academicService.getStudentAcademicOverview(targetId));

        return "student/marks";
    }

    @GetMapping("/profile")
    public String myProfile(HttpSession session, Model model) {
        UserSession user = getUser(session);
        model.addAttribute("user", user);
        return "student/profile";
    }

    @GetMapping("/activity")
    public String myActivity(HttpSession session, Model model) {
        UserSession user = getUser(session);
        model.addAttribute("activities", activityLogService.getRecentLogsByActor(user.getUsername(), 20));
        return "student/activity";
    }
}
