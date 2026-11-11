package com.netsentinel.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Global Exception Handler for NetSentinel Web Application.
 * 
 * Demonstrates:
 * - Centralized Spring @ControllerAdvice exception handling
 * - Defensive security: Never exposing raw stack traces or internal queries
 * - Mapping custom domain exceptions to appropriate HTTP response codes
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UnauthorizedAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorizedAccess(UnauthorizedAccessException ex, HttpServletRequest request, Model model) {
        log.warn("Access violation intercepted: actor={}, resource={}, message={}",
                ex.getActorUsername(), ex.getAttemptedResource(), ex.getMessage());
        model.addAttribute("errorCode", ex.getErrorCode());
        model.addAttribute("deniedReason", ex.getMessage());
        model.addAttribute("timestamp", ex.getTimestamp());
        model.addAttribute("currentUri", request.getRequestURI());
        return "error/403";
    }

    @ExceptionHandler(SecurityViolationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleSecurityViolation(SecurityViolationException ex, HttpServletRequest request, Model model) {
        log.warn("Security policy breach: rule={}, severity={}, message={}",
                ex.getRuleCode(), ex.getSeverity(), ex.getMessage());
        model.addAttribute("errorCode", ex.getErrorCode());
        model.addAttribute("deniedReason", "Security violation detected [" + ex.getRuleCode() + "]: " + ex.getMessage());
        model.addAttribute("timestamp", ex.getTimestamp());
        model.addAttribute("currentUri", request.getRequestURI());
        return "error/403";
    }

    @ExceptionHandler(AccountInactiveException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleAccountInactive(AccountInactiveException ex, Model model) {
        log.warn("Inactive account login attempt: user={}", ex.getUsername());
        model.addAttribute("errorMessage", "Account is inactive or suspended. Please contact the administrator.");
        return "auth/login";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request, Model model) {
        log.warn("Resource missing: type={}, id={}", ex.getResourceType(), ex.getResourceId());
        model.addAttribute("notFoundMessage", ex.getMessage());
        model.addAttribute("currentUri", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled server exception at {}", request.getRequestURI(), ex);
        model.addAttribute("errorMessage", "An internal system error occurred while processing your request.");
        model.addAttribute("currentUri", request.getRequestURI());
        return "error/404";
    }
}

