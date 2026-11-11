package com.netsentinel.web.security;

import com.netsentinel.web.model.UserRole;
import com.netsentinel.web.service.AuditLogService;
import com.netsentinel.web.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public AuthInterceptor(AuditLogService auditLogService, NotificationService notificationService) {
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // Allow public static resources, error pages, auth endpoints, and desktop api
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/icons/") || 
            uri.startsWith("/favicon.ico") || uri.equals("/login") || uri.startsWith("/auth/") ||
            uri.equals("/error") || uri.equals("/403") || uri.equals("/404") || uri.equals("/500") ||
            uri.startsWith("/api/v1/desktop")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        UserSession user = (session != null) ? (UserSession) session.getAttribute("userSession") : null;

        if (user == null) {
            response.sendRedirect("/login?error=unauthenticated");
            return false;
        }

        // Role-based Access Control Enforcement
        if (uri.startsWith("/admin")) {
            if (user.getRole() != UserRole.ADMIN) {
                // Security anomaly: Unauthorized non-admin access to admin zone!
                Map<String, Object> attrs = new HashMap<>();
                attrs.put("simulation_privilege_escalation", true);
                auditLogService.logAndInspect(user.getId(), user.getUsername(), user.getRole().name(),
                        "UNAUTHORIZED_ADMIN_ZONE_ACCESS", uri, "BLOCKED", request.getRemoteAddr(),
                        request.getHeader("User-Agent"), "Non-admin account attempted direct URL navigation to " + uri, attrs);

                response.sendRedirect("/403?denied=admin_access");
                return false;
            }
        } else if (uri.startsWith("/faculty")) {
            if (user.getRole() != UserRole.FACULTY && user.getRole() != UserRole.ADMIN) {
                response.sendRedirect("/403?denied=faculty_access");
                return false;
            }
        } else if (uri.startsWith("/student")) {
            if (user.getRole() != UserRole.STUDENT && user.getRole() != UserRole.ADMIN) {
                response.sendRedirect("/403?denied=student_access");
                return false;
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null && !modelAndView.getViewName().startsWith("redirect:")) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                UserSession user = (UserSession) session.getAttribute("userSession");
                if (user != null) {
                    modelAndView.addObject("currentUser", user);
                    int unread = notificationService.getUnreadCount(user.getRole().name());
                    modelAndView.addObject("unreadCount", unread);
                    modelAndView.addObject("unreadNotifications", notificationService.getUnreadNotifications(user.getRole().name()));
                }
            }
            modelAndView.addObject("currentUri", request.getRequestURI());
        }
    }
}

