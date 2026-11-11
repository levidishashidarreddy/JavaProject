package com.netsentinel.web.controller;

import com.netsentinel.web.security.UserSession;
import com.netsentinel.web.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String root(HttpSession session) {
        UserSession user = (session != null) ? (UserSession) session.getAttribute("userSession") : null;
        if (user != null) {
            return switch (user.getRole()) {
                case STUDENT -> "redirect:/student/dashboard";
                case FACULTY -> "redirect:/faculty/dashboard";
                case ADMIN -> "redirect:/admin/dashboard";
            };
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            HttpSession session,
                            Model model) {
        UserSession user = (session != null) ? (UserSession) session.getAttribute("userSession") : null;
        if (user != null) {
            return root(session);
        }

        if ("invalid".equals(error)) {
            model.addAttribute("errorMessage", "Invalid credentials. Please verify your username and password.");
        } else if ("unauthenticated".equals(error)) {
            model.addAttribute("warningMessage", "Authentication required. Please sign in to access portal resources.");
        }

        if (logout != null) {
            model.addAttribute("successMessage", "You have been signed out securely.");
        }

        return "auth/login";
    }

    @PostMapping("/auth/login")
    public String processLogin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpServletRequest request,
                               HttpSession session) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        Optional<UserSession> userOpt = authService.authenticate(username, password, ip, userAgent);
        if (userOpt.isPresent()) {
            UserSession user = userOpt.get();
            session.setAttribute("userSession", user);
            return switch (user.getRole()) {
                case STUDENT -> "redirect:/student/dashboard";
                case FACULTY -> "redirect:/faculty/dashboard";
                case ADMIN -> "redirect:/admin/dashboard";
            };
        }

        return "redirect:/login?error=invalid";
    }

    @PostMapping("/auth/demo-login")
    public String demoLogin(@RequestParam("role") String role,
                            HttpServletRequest request,
                            HttpSession session) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        Optional<UserSession> userOpt = authService.demoLogin(role, ip, userAgent);
        if (userOpt.isPresent()) {
            UserSession user = userOpt.get();
            session.setAttribute("userSession", user);
            return switch (user.getRole()) {
                case STUDENT -> "redirect:/student/dashboard";
                case FACULTY -> "redirect:/faculty/dashboard";
                case ADMIN -> "redirect:/admin/dashboard";
            };
        }

        return "redirect:/login?error=invalid";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }

    @GetMapping("/403")
    public String accessDenied(@RequestParam(value = "denied", required = false) String denied, Model model) {
        model.addAttribute("deniedReason", denied != null ? denied : "Restricted security zone.");
        return "error/403";
    }
}

