package com.netsentinel.web.engine;

import com.netsentinel.web.engine.rules.DetectionRule;
import com.netsentinel.web.model.DetectionRuleConfig;
import com.netsentinel.web.model.Incident;
import com.netsentinel.web.model.IncidentEvent;
import com.netsentinel.web.model.Notification;
import com.netsentinel.web.repository.DetectionRuleRepository;
import com.netsentinel.web.repository.IncidentRepository;
import com.netsentinel.web.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SecurityDetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(SecurityDetectionEngine.class);

    private final List<DetectionRule> rules;
    private final DetectionRuleRepository ruleRepository;
    private final IncidentRepository incidentRepository;
    private final NotificationRepository notificationRepository;
    private final com.netsentinel.web.service.FirebaseDatabaseService firebaseDatabaseService;

    public SecurityDetectionEngine(List<DetectionRule> rules,
                                   DetectionRuleRepository ruleRepository,
                                   IncidentRepository incidentRepository,
                                   NotificationRepository notificationRepository,
                                   com.netsentinel.web.service.FirebaseDatabaseService firebaseDatabaseService) {
        this.rules = rules;
        this.ruleRepository = ruleRepository;
        this.incidentRepository = incidentRepository;
        this.notificationRepository = notificationRepository;
        this.firebaseDatabaseService = firebaseDatabaseService;
    }

    public Optional<Incident> evaluate(DetectionContext context) {
        log.debug("Evaluating security detection engine for actor={}, action={}, resource={}",
                context.getActorUsername(), context.getAction(), context.getTargetResource());

        Map<String, DetectionRuleConfig> configMap = new HashMap<>();
        for (DetectionRuleConfig cfg : ruleRepository.findAll()) {
            configMap.put(cfg.getRuleCode().toUpperCase(), cfg);
        }

        List<DetectionRule> sortedRules = new ArrayList<>(rules);
        sortedRules.sort(Comparator.comparing(DetectionRule::getRuleCode));

        for (DetectionRule rule : sortedRules) {
            DetectionRuleConfig cfg = configMap.get(rule.getRuleCode().toUpperCase());
            if (cfg != null && !cfg.isEnabled()) {
                continue; // Rule disabled by administrator
            }

            try {
                DetectionResult result = rule.evaluate(context, cfg);
                if (result != null && result.isDetected()) {
                    log.warn("SECURITY ANOMALY DETECTED by rule [{}]: {}", rule.getRuleCode(), result.getTitle());
                    Incident incident = createIncidentFromDetection(context, result);
                    return Optional.of(incident);
                }
            } catch (Exception e) {
                log.error("Error evaluating rule {}", rule.getRuleCode(), e);
            }
        }

        return Optional.empty();
    }

    public Incident createIncidentFromDetection(DetectionContext context, DetectionResult result) {
        String incNum = "INC-2026-" + String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 9999));

        Incident incident = new Incident();
        incident.setIncidentNumber(incNum);
        incident.setTitle(result.getTitle());
        incident.setCategory(result.getCategory());
        incident.setSeverity(result.getSeverity());
        incident.setStatus("OPEN");
        incident.setActorId(context.getActorId());
        incident.setActorUsername(context.getActorUsername());
        incident.setActorRole(context.getActorRole());
        incident.setTargetResource(context.getTargetResource());
        incident.setRuleCode(result.getRuleCode());
        incident.setConfidenceScore(result.getConfidenceScore());
        incident.setReason(result.getReason());
        incident.setEvidenceJson(result.getEvidenceJson());
        incident.setInsightExplanation(result.getInsightExplanation());
        incident.setRecommendedAction(result.getRecommendedAction());
        incident.setAssignedTo("Admin Dave");
        incident.setCreatedAt(LocalDateTime.now());
        incident.setUpdatedAt(LocalDateTime.now());

        Long incId = incidentRepository.insert(incident);
        incident.setId(incId);
        if (firebaseDatabaseService != null) {
            firebaseDatabaseService.syncIncident(incident);
        }

        // Timeline events
        IncidentEvent ev1 = new IncidentEvent(incId, "ANOMALY_DETECTED", null, "SYSTEM_ENGINE",
                "Rule " + result.getRuleCode() + " triggered: " + result.getReason(), LocalDateTime.now());
        incidentRepository.insertEvent(ev1);

        IncidentEvent ev2 = new IncidentEvent(incId, "INCIDENT_CREATED", null, "SYSTEM_ENGINE",
                "Incident " + incNum + " registered with " + result.getSeverity() + " severity (Confidence " + result.getConfidenceScore() + "%).", LocalDateTime.now());
        incidentRepository.insertEvent(ev2);

        // Admin notification
        Notification notif = new Notification();
        notif.setRecipientRole("ADMIN");
        notif.setTitle(result.getSeverity() + " Security Incident: " + incNum);
        notif.setMessage(result.getTitle() + " originating from " + context.getActorUsername() + " (" + context.getIpAddress() + ")");
        notif.setSeverity(result.getSeverity());
        notif.setReadStatus(false);
        notif.setLink("/admin/incidents/" + incId);
        notif.setCreatedAt(LocalDateTime.now());
        notificationRepository.insert(notif);
        if (firebaseDatabaseService != null) {
            firebaseDatabaseService.syncNotification(notif);
        }

        return incident;
    }
}

