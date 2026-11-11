package com.netsentinel.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.netsentinel.web.config.FirebaseConfig;
import com.netsentinel.web.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * NetSentinel Firebase Realtime Database Service.
 * 
 * Manages bi-directional synchronization between NetSentinel Spring Boot backend
 * and the Google Firebase Realtime Database (https://netsentinel-1ded9-default-rtdb.firebaseio.com).
 * 
 * Architecture:
 * - Primary: Google Firebase Admin SDK 9.3.0 (when service account credentials supplied)
 * - Secondary / REST: Async non-blocking Java 21 HttpClient REST synchronizer
 * - Local Master: SQLite continuous database storage
 */
@Service
public class FirebaseDatabaseService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseDatabaseService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final FirebaseDatabase firebaseDatabase;
    private final FirebaseConfig firebaseConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public FirebaseDatabaseService(@Autowired(required = false) FirebaseDatabase firebaseDatabase,
                                   FirebaseConfig firebaseConfig) {
        this.firebaseDatabase = firebaseDatabase;
        this.firebaseConfig = firebaseConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isConnected() {
        return (firebaseDatabase != null && firebaseConfig.isInitialized()) || firebaseConfig.isFirebaseEnabled();
    }

    public String getConnectionStatus() {
        if (firebaseDatabase != null && firebaseConfig.isInitialized()) {
            return "CONNECTED_ADMIN_SDK";
        }
        if (firebaseConfig.isFirebaseEnabled()) {
            return "CONNECTED_REST_SYNC";
        }
        return "LOCAL_SQLITE_MODE";
    }

    public Map<String, Object> getDiagnosticInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", getConnectionStatus());
        info.put("connected", isConnected());
        info.put("projectId", firebaseConfig.getProjectId());
        info.put("databaseUrl", firebaseConfig.getDatabaseUrl());
        info.put("authDomain", firebaseConfig.getAuthDomain());
        info.put("cloudProvider", "Google Cloud / Firebase Realtime Database");
        info.put("backendEngine", "Java 21 Firebase Hybrid Connector");
        info.put("timestamp", LocalDateTime.now().format(ISO_FORMATTER));
        return info;
    }

    // ------------------------------------------------------------------------
    // Synchronization Operations
    // ------------------------------------------------------------------------

    public CompletableFuture<Boolean> syncUser(User user) {
        if (!isConnected() || user == null) {
            return CompletableFuture.completedFuture(false);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("userCode", user.getUserCode());
        data.put("username", user.getUsername());
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole());
        data.put("department", user.getDepartment());
        data.put("status", user.getStatus());
        data.put("updatedAt", LocalDateTime.now().format(ISO_FORMATTER));

        return writeNode("users/" + user.getUsername(), data);
    }

    public CompletableFuture<Boolean> syncAttendance(Attendance att) {
        if (!isConnected() || att == null) {
            return CompletableFuture.completedFuture(false);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", att.getId());
        data.put("studentId", att.getStudentId());
        data.put("subjectId", att.getSubjectId());
        data.put("facultyId", att.getFacultyId());
        data.put("date", att.getDate() != null ? att.getDate().toString() : "");
        data.put("status", att.getStatus());
        data.put("remarks", att.getRemarks());
        data.put("recordedAt", att.getRecordedAt() != null ? att.getRecordedAt().format(ISO_FORMATTER) : LocalDateTime.now().format(ISO_FORMATTER));

        return writeNode("attendance/" + (att.getId() != null ? att.getId() : System.currentTimeMillis()), data);
    }

    public CompletableFuture<Boolean> syncMark(Mark mark) {
        if (!isConnected() || mark == null) {
            return CompletableFuture.completedFuture(false);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", mark.getId());
        data.put("studentId", mark.getStudentId());
        data.put("subjectId", mark.getSubjectId());
        data.put("facultyId", mark.getFacultyId());
        data.put("examType", mark.getExamType());
        data.put("score", mark.getScore());
        data.put("maxScore", mark.getMaxScore());
        data.put("recordedAt", mark.getRecordedAt() != null ? mark.getRecordedAt().format(ISO_FORMATTER) : LocalDateTime.now().format(ISO_FORMATTER));

        return writeNode("marks/" + (mark.getId() != null ? mark.getId() : System.currentTimeMillis()), data);
    }

    public CompletableFuture<Boolean> syncIncident(Incident inc) {
        if (!isConnected() || inc == null) {
            return CompletableFuture.completedFuture(false);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", inc.getId());
        data.put("incidentNumber", inc.getIncidentNumber());
        data.put("title", inc.getTitle());
        data.put("category", inc.getCategory());
        data.put("severity", inc.getSeverity());
        data.put("status", inc.getStatus());
        data.put("actorUsername", inc.getActorUsername());
        data.put("actorRole", inc.getActorRole());
        data.put("targetResource", inc.getTargetResource());
        data.put("ruleCode", inc.getRuleCode());
        data.put("confidenceScore", inc.getConfidenceScore());
        data.put("reason", inc.getReason());
        data.put("insightExplanation", inc.getInsightExplanation());
        data.put("recommendedAction", inc.getRecommendedAction());
        data.put("createdAt", inc.getCreatedAt() != null ? inc.getCreatedAt().format(ISO_FORMATTER) : LocalDateTime.now().format(ISO_FORMATTER));

        String key = inc.getIncidentNumber() != null ? inc.getIncidentNumber() : "INC-" + System.currentTimeMillis();
        return writeNode("incidents/" + key, data);
    }

    public CompletableFuture<Boolean> syncActivityLog(ActivityLog logEntry) {
        if (!isConnected() || logEntry == null) {
            return CompletableFuture.completedFuture(false);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", logEntry.getId());
        data.put("actorId", logEntry.getActorId());
        data.put("actorUsername", logEntry.getActorUsername());
        data.put("actorRole", logEntry.getActorRole());
        data.put("action", logEntry.getAction());
        data.put("targetResource", logEntry.getTargetResource());
        data.put("outcome", logEntry.getOutcome());
        data.put("ipAddress", logEntry.getIpAddress());
        data.put("requestId", logEntry.getRequestId());
        data.put("timestamp", logEntry.getTimestamp() != null ? logEntry.getTimestamp().format(ISO_FORMATTER) : LocalDateTime.now().format(ISO_FORMATTER));

        String key = logEntry.getRequestId() != null ? logEntry.getRequestId() : String.valueOf(System.currentTimeMillis());
        return writeNode("activityLogs/" + key, data);
    }

    public CompletableFuture<Boolean> syncNotification(Notification notif) {
        if (!isConnected() || notif == null) {
            return CompletableFuture.completedFuture(false);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", notif.getId());
        data.put("recipientRole", notif.getRecipientRole());
        data.put("title", notif.getTitle());
        data.put("message", notif.getMessage());
        data.put("severity", notif.getSeverity());
        data.put("readStatus", notif.isReadStatus());
        data.put("createdAt", notif.getCreatedAt() != null ? notif.getCreatedAt().format(ISO_FORMATTER) : LocalDateTime.now().format(ISO_FORMATTER));

        return writeNode("notifications/" + (notif.getId() != null ? notif.getId() : System.currentTimeMillis()), data);
    }

    // ------------------------------------------------------------------------
    // Node Writer (Admin SDK or REST Fallback)
    // ------------------------------------------------------------------------

    private CompletableFuture<Boolean> writeNode(String path, Object payload) {
        // Path 1: Firebase Admin SDK (High performance persistent socket)
        if (firebaseDatabase != null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            try {
                DatabaseReference ref = firebaseDatabase.getReference(path);
                ref.setValue(payload, (databaseError, databaseReference) -> {
                    if (databaseError != null) {
                        log.warn("[FIREBASE-ADMIN-ERROR] Failed to write path '{}': {}", path, databaseError.getMessage());
                        future.complete(false);
                    } else {
                        log.debug("[FIREBASE-ADMIN-SUCCESS] Node '{}' synchronized successfully.", path);
                        future.complete(true);
                    }
                });
            } catch (Exception e) {
                log.warn("[FIREBASE-ADMIN-EXCEPTION] Error during node write to '{}': {}", path, e.getMessage());
                future.complete(false);
            }
            return future;
        }

        // Path 2: Java 21 Non-blocking Async HTTP REST Sync
        String databaseUrl = firebaseConfig.getDatabaseUrl();
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            try {
                String jsonPayload = objectMapper.writeValueAsString(payload);
                String targetUrl = databaseUrl.replaceAll("/+$", "") + "/" + path + ".json";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .timeout(Duration.ofSeconds(3))
                        .build();

                return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(resp -> {
                            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                                log.debug("[FIREBASE-REST-SUCCESS] Synced path '{}' -> HTTP {}", path, resp.statusCode());
                                return true;
                            } else {
                                log.debug("[FIREBASE-REST-INFO] Path '{}' HTTP {}: {}", path, resp.statusCode(), resp.body());
                                return false;
                            }
                        })
                        .exceptionally(ex -> {
                            log.debug("[FIREBASE-REST-NOTICE] Sync request for '{}': {}", path, ex.getMessage());
                            return false;
                        });
            } catch (Exception e) {
                log.debug("[FIREBASE-REST-EXCEPTION] Serialization/HTTP failure for '{}': {}", path, e.getMessage());
                return CompletableFuture.completedFuture(false);
            }
        }

        return CompletableFuture.completedFuture(false);
    }
}
