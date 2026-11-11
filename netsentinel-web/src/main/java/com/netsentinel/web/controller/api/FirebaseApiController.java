package com.netsentinel.web.controller.api;

import com.netsentinel.web.config.FirebaseConfig;
import com.netsentinel.web.service.FirebaseDatabaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * NetSentinel Firebase Client API Controller.
 * 
 * Exposes Firebase configuration and status for the frontend JavaScript client,
 * enabling direct browser telemetry and real-time event updates.
 */
@RestController
@RequestMapping("/api/v1/firebase")
@CrossOrigin(origins = "*")
public class FirebaseApiController {

    private final FirebaseConfig firebaseConfig;
    private final FirebaseDatabaseService firebaseDatabaseService;

    public FirebaseApiController(FirebaseConfig firebaseConfig, FirebaseDatabaseService firebaseDatabaseService) {
        this.firebaseConfig = firebaseConfig;
        this.firebaseDatabaseService = firebaseDatabaseService;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getFirebaseConfig() {
        return ResponseEntity.ok(firebaseConfig.getClientConfig());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFirebaseStatus() {
        return ResponseEntity.ok(firebaseDatabaseService.getDiagnosticInfo());
    }
}

