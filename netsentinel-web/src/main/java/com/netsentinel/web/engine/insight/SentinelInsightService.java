package com.netsentinel.web.engine.insight;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netsentinel.web.model.ActivityLog;
import com.netsentinel.web.model.DetectionRuleConfig;
import com.netsentinel.web.model.Incident;
import com.netsentinel.web.repository.ActivityLogRepository;
import com.netsentinel.web.repository.DetectionRuleRepository;
import com.netsentinel.web.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Sentinel Insight Engine Service.
 * 
 * Generates transparent, deterministic, and explainable intelligence dossiers
 * analyzing real incidents and supporting activity logs across 8 structured dimensions.
 * Does not require external AI APIs.
 */
@Service
public class SentinelInsightService {

    private final IncidentRepository incidentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final DetectionRuleRepository detectionRuleRepository;
    private final ObjectMapper objectMapper;

    public SentinelInsightService(IncidentRepository incidentRepository,
                                  ActivityLogRepository activityLogRepository,
                                  DetectionRuleRepository detectionRuleRepository,
                                  ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.activityLogRepository = activityLogRepository;
        this.detectionRuleRepository = detectionRuleRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<ExplainableDossier> generateDossier(Long incidentId) {
        Optional<Incident> incOpt = incidentRepository.findById(incidentId);
        return incOpt.map(this::buildDossierFromIncident);
    }

    public Optional<ExplainableDossier> generateDossierByNumber(String incidentNumber) {
        Optional<Incident> incOpt = incidentRepository.findByIncidentNumber(incidentNumber);
        return incOpt.map(this::buildDossierFromIncident);
    }

    public ExplainableDossier buildDossierFromIncident(Incident inc) {
        ExplainableDossier dossier = new ExplainableDossier();
        dossier.setIncidentNumber(inc.getIncidentNumber());
        dossier.setTitle(inc.getTitle());
        dossier.setRuleCode(inc.getRuleCode());
        dossier.setSeverity(inc.getSeverity());
        dossier.setStatus(inc.getStatus());
        dossier.setTimestamp(inc.getCreatedAt() != null ? inc.getCreatedAt().toString().replace("T", " ") : "N/A");

        Optional<DetectionRuleConfig> cfgOpt = detectionRuleRepository.findByCode(inc.getRuleCode());
        String ruleName = cfgOpt.map(DetectionRuleConfig::getRuleName).orElse(inc.getTitle());
        dossier.setRuleName(ruleName);

        // Parse evidence JSON
        Map<String, Object> evidenceMap = new LinkedHashMap<>();
        if (inc.getEvidenceJson() != null && !inc.getEvidenceJson().isBlank()) {
            try {
                evidenceMap = objectMapper.readValue(inc.getEvidenceJson(), new TypeReference<>() {});
            } catch (Exception e) {
                evidenceMap.put("raw_evidence", inc.getEvidenceJson());
            }
        }
        evidenceMap.put("recorded_timestamp", dossier.getTimestamp());
        evidenceMap.put("incident_id", inc.getIncidentNumber());
        dossier.setEvidenceDetails(evidenceMap);

        // Fetch surrounding activity logs
        List<ActivityLog> recentLogs = activityLogRepository.findRecentByActor(inc.getActorUsername(), 10);
        List<Map<String, String>> logSummaries = new ArrayList<>();
        for (ActivityLog log : recentLogs) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("time", log.getTimestamp() != null ? log.getTimestamp().toString().replace("T", " ") : "");
            m.put("action", log.getAction());
            m.put("outcome", log.getOutcome());
            m.put("resource", log.getTargetResource());
            m.put("details", log.getDetails() != null ? log.getDetails() : "");
            logSummaries.add(m);
        }
        dossier.setRelatedActivityLogs(logSummaries);

        // 1. What happened?
        dossier.setWhatHappened(determineWhatHappened(inc, evidenceMap));

        // 2. Who performed the action?
        dossier.setWhoPerformedAction(String.format("User '%s' holding assigned role [%s] (Database ID: %s).",
                inc.getActorUsername(), inc.getActorRole(), inc.getActorId() != null ? inc.getActorId() : "Unknown"));

        // 3. Which resource was targeted?
        dossier.setResourceTargeted(String.format("Target Resource: '%s'. Category: [%s].",
                inc.getTargetResource(), inc.getCategory()));

        // 4. Why was the event flagged?
        dossier.setWhyFlagged(inc.getReason() != null ? inc.getReason() : 
                "Operation deviated from established role boundary, course roster, or transaction velocity thresholds.");

        // 5. Which detection rule was triggered?
        int threshold = cfgOpt.map(DetectionRuleConfig::getThreshold).orElse(1);
        int window = cfgOpt.map(DetectionRuleConfig::getTimeWindowSeconds).orElse(60);
        dossier.setRuleTriggeredDetails(String.format("Triggered Rule: %s (%s). Category: %s. Active Threshold: %d event(s) in %d seconds.",
                inc.getRuleCode(), ruleName, inc.getCategory(), threshold, window));

        // 7. Potential impact?
        dossier.setPotentialImpact(determinePotentialImpact(inc.getRuleCode()));

        // 8. Recommended investigation next steps?
        dossier.setRecommendedNextSteps(determineRecommendedSteps(inc, evidenceMap));

        return dossier;
    }

    private String determineWhatHappened(Incident inc, Map<String, Object> evidence) {
        return switch (inc.getRuleCode()) {
            case "RULE-ATT-001" -> {
                String sub = (String) evidence.getOrDefault("target_subject", "Course");
                String stu = (String) evidence.getOrDefault("target_student", "Roster");
                yield String.format("A request was intercepted attempting to record or alter attendance records for subject '%s' (%s) without authorized faculty instructional assignment.", sub, stu);
            }
            case "RULE-MOD-002" -> {
                Object count = evidence.getOrDefault("burst_count", "rapid");
                Object win = evidence.getOrDefault("time_window_seconds", "short");
                yield String.format("A high-velocity burst of %s academic grade or attendance modifications was recorded within %s seconds, exceeding standard baseline velocity limits.", count, win);
            }
            case "RULE-PRV-003" -> {
                String targetAcc = (String) evidence.getOrDefault("target_account", "Account");
                String reqRole = (String) evidence.getOrDefault("requested_role", "ADMIN");
                yield String.format("A non-administrative session attempted to execute a role elevation or privilege change on '%s' requesting role [%s].", targetAcc, reqRole);
            }
            case "RULE-REC-004" -> {
                Object targetStu = evidence.getOrDefault("target_student_id", "external student");
                yield String.format("An unauthorized query attempted to access student transcript and grade records for Student ID %s outside permitted ownership or faculty course assignment scope.", targetStu);
            }
            case "RULE-TIM-005" -> "An administrative or gradebook alteration was executed during configured off-hours quiet window (02:00 - 05:00 AM).";
            case "RULE-BRT-006" -> "Multiple failed authentication attempts were recorded against campus accounts from the same network origin in a short duration.";
            default -> inc.getTitle() + ": " + inc.getReason();
        };
    }

    private String determinePotentialImpact(String ruleCode) {
        return switch (ruleCode) {
            case "RULE-ATT-001" -> "Distortion of student attendance compliance, accreditation audit discrepancies, or unauthorized clearance for examination eligibility.";
            case "RULE-MOD-002" -> "Compromised gradebook integrity, erroneous GPA recalculations, or undetected tampering across multiple student course transcripts.";
            case "RULE-PRV-003" -> "Critical privilege escalation leading to complete administrative portal takeover, arbitrary user provisioning, and unauthorized policy modifications.";
            case "RULE-REC-004" -> "Violation of academic privacy standards (FERPA), unauthorized student data harvesting, and potential identity profiling.";
            case "RULE-TIM-005" -> "Undetected stealth actions outside normal operating hours, potentially indicating compromised unattended workstations.";
            case "RULE-BRT-006" -> "Credential compromise, account lockout denial-of-service, or automated dictionary intrusion.";
            default -> "Potential risk to academic system confidentiality, integrity, or availability.";
        };
    }

    private List<String> determineRecommendedSteps(Incident inc, Map<String, Object> evidence) {
        List<String> steps = new ArrayList<>();
        steps.add("1. Review related activity logs from actor '" + inc.getActorUsername() + "' spanning 15 minutes before and after the event.");
        steps.add("2. Verify whether origin IP address (" + evidence.getOrDefault("ip_address", "127.0.0.1") + ") matches the user's expected campus subnet or VPN pool.");
        
        switch (inc.getRuleCode()) {
            case "RULE-ATT-001" -> {
                steps.add("3. Cross-reference academic departmental schedules to confirm whether a substitute teaching assignment was recently approved.");
                steps.add("4. Verify signed paper rosters or physical biometric records for the corresponding lecture session.");
            }
            case "RULE-MOD-002" -> {
                steps.add("3. Contact the faculty member to confirm if this was an authorized bulk grade upload or automated spreadsheet import.");
                steps.add("4. Check database transaction logs to determine if score rollbacks are necessary.");
            }
            case "RULE-PRV-003" -> {
                steps.add("3. Immediately terminate any active HTTP sessions for account '" + inc.getActorUsername() + "'.");
                steps.add("4. Audit adjacent user accounts to verify no other unauthorized role elevations occurred.");
            }
            case "RULE-REC-004" -> {
                steps.add("3. Inspect client browser session headers for automated scraping scripts (e.g., Python-requests, Puppeteer).");
                steps.add("4. Confirm whether student ID enumeration occurred across sequential numerical parameters.");
            }
            default -> {
                steps.add("3. Check for repeated security warnings associated with this actor or IP.");
                steps.add("4. If malicious intent is suspected, temporarily suspend account access and notify department chair.");
            }
        }
        return steps;
    }
}

