package com.netsentinel.web.controller;

import com.netsentinel.web.config.FirebaseConfig;
import com.netsentinel.web.service.FirebaseDatabaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    private final FirebaseConfig firebaseConfig;
    private final FirebaseDatabaseService firebaseDatabaseService;

    public SettingsController(FirebaseConfig firebaseConfig, FirebaseDatabaseService firebaseDatabaseService) {
        this.firebaseConfig = firebaseConfig;
        this.firebaseDatabaseService = firebaseDatabaseService;
    }

    @GetMapping
    public String viewSettings(Model model) {
        model.addAttribute("databaseUrl", databaseUrl);
        model.addAttribute("appVersion", "1.0.0-PROD-CANDIDATE");
        model.addAttribute("engineStatus", "ONLINE / LOW LATENCY");
        model.addAttribute("sqliteMode", "WAL (Write-Ahead Logging)");
        
        // Firebase Cloud Parameters
        model.addAttribute("firebaseProjectId", firebaseConfig.getProjectId());
        model.addAttribute("firebaseDatabaseUrl", firebaseConfig.getDatabaseUrl());
        model.addAttribute("firebaseAuthDomain", firebaseConfig.getAuthDomain());
        model.addAttribute("firebaseAppId", firebaseConfig.getAppId());
        model.addAttribute("firebaseStatus", firebaseDatabaseService.getConnectionStatus());
        model.addAttribute("firebaseConnected", firebaseDatabaseService.isConnected());
        return "admin/settings";
    }

    @PostMapping("/save")
    public String saveSettings(@RequestParam(value = "notificationsEnabled", defaultValue = "true") boolean notifs,
                               @RequestParam(value = "autoInvestigation", defaultValue = "true") boolean autoInvestigate) {
        return "redirect:/admin/settings?success=saved";
    }
}
