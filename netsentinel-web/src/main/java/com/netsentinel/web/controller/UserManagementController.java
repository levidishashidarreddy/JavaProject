package com.netsentinel.web.controller;

import com.netsentinel.web.model.User;
import com.netsentinel.web.security.UserSession;
import com.netsentinel.web.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(@RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "role", required = false) String roleFilter,
                            Model model) {
        List<User> users = userService.searchUsers(search, roleFilter);
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("roleFilter", roleFilter);
        return "admin/users";
    }

    @PostMapping("/add")
    public String addUser(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          @RequestParam("fullName") String fullName,
                          @RequestParam("email") String email,
                          @RequestParam("role") String roleStr,
                          @RequestParam("department") String department,
                          HttpSession session,
                          HttpServletRequest request) {
        UserSession admin = (UserSession) session.getAttribute("userSession");

        try {
            userService.createDemoUser(username, password, fullName, email, roleStr, department,
                    admin.getId(), admin.getUsername(), request.getRemoteAddr());
            return "redirect:/admin/users?success=created";
        } catch (Exception e) {
            return "redirect:/admin/users?error=duplicate";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Long id,
                               HttpSession session,
                               HttpServletRequest request) {
        UserSession admin = (UserSession) session.getAttribute("userSession");
        userService.toggleUserStatus(admin.getId(), admin.getUsername(), admin.getRole().name(), id, request.getRemoteAddr());
        return "redirect:/admin/users?success=status_changed";
    }

    @PostMapping("/{id}/role")
    public String updateRole(@PathVariable("id") Long id,
                             @RequestParam("role") String newRole,
                             HttpSession session,
                             HttpServletRequest request) {
        UserSession admin = (UserSession) session.getAttribute("userSession");
        userService.updateUserRole(admin.getId(), admin.getUsername(), admin.getRole().name(), id, newRole, request.getRemoteAddr());
        return "redirect:/admin/users?success=role_updated";
    }
}
