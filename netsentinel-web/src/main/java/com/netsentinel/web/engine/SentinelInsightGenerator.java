package com.netsentinel.web.engine;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SentinelInsightGenerator {

    public String generateInsight(String ruleCode, String actorUsername, String actorRole, String targetResource, int confidenceScore, String contextDetails) {
        StringBuilder sb = new StringBuilder();
        String timeNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        switch (ruleCode) {
            case "RULE-ATT-001" -> {
                sb.append("SENTINEL BEHAVIORAL ANALYSIS [Timestamp: ").append(timeNow).append("]\n\n")
                  .append("1. Anomaly Vector: Unauthorized Attendance Mutation\n")
                  .append("The portal engine intercepted an attendance modification request targeting '").append(targetResource).append("'. ")
                  .append("The session identity '").append(actorUsername).append("' holds the role [").append(actorRole).append("], ")
                  .append("which violates academic separation-of-duty boundaries. Students lack role authorization, and faculty members are constrained exclusively to their assigned departmental courses.\n\n")
                  .append("2. Risk Assessment (Confidence: ").append(confidenceScore).append("%):\n")
                  .append("Direct state tampering detected. Potential attack vectors include parameter manipulation via web devtools, API route replay, or an unauthenticated proxy script.\n\n")
                  .append("3. Threat Classification:\n")
                  .append("MITRE ATT&CK: T1078 (Valid Accounts / Abuse of Portal Endpoints), T1565.001 (Stored Data Manipulation).\n\n")
                  .append("4. Anomaly Context:\n")
                  .append(contextDetails);
            }
            case "RULE-MOD-002" -> {
                sb.append("SENTINEL BEHAVIORAL ANALYSIS [Timestamp: ").append(timeNow).append("]\n\n")
                  .append("1. Anomaly Vector: Rapid Academic Grade Manipulation (Velocity Spike)\n")
                  .append("A burst of sequential score modifications was submitted by '").append(actorUsername).append("' across multiple gradebook entities. ")
                  .append("The transaction frequency exceeds human typing baseline velocity (> 3 adjustments per minute). This deviation indicates either automated script execution, botnet replay, or credential harvesting.\n\n")
                  .append("2. Risk Assessment (Confidence: ").append(confidenceScore).append("%):\n")
                  .append("High-impact integrity hazard. If unverified, compromised credentials could silently distort academic standing or GPA calculation matrices.\n\n")
                  .append("3. Threat Classification:\n")
                  .append("MITRE ATT&CK: T1078.004 (Cloud/Web Session Hijacking), T1565 (Data Manipulation).\n\n")
                  .append("4. Anomaly Context:\n")
                  .append(contextDetails);
            }
            case "RULE-PRV-003" -> {
                sb.append("SENTINEL BEHAVIORAL ANALYSIS [Timestamp: ").append(timeNow).append("]\n\n")
                  .append("1. Anomaly Vector: Unauthorized Privilege Escalation Probe\n")
                  .append("User '").append(actorUsername).append("' (Role: ").append(actorRole).append(") executed a management mutation targeting administrative endpoints. ")
                  .append("Administrative functions (account elevation, role alterations, status toggles) are strictly restricted to verified SOC Administrator sessions.\n\n")
                  .append("2. Risk Assessment (Confidence: ").append(confidenceScore).append("%):\n")
                  .append("Critical control boundary breach. Suggests an adversary actively surveying perimeter privileges or testing IDOR (Insecure Direct Object Reference) vulnerabilities.\n\n")
                  .append("3. Threat Classification:\n")
                  .append("MITRE ATT&CK: T1068 (Exploitation for Privilege Escalation), T1098 (Account Manipulation).\n\n")
                  .append("4. Anomaly Context:\n")
                  .append(contextDetails);
            }
            case "RULE-REC-004" -> {
                sb.append("SENTINEL BEHAVIORAL ANALYSIS [Timestamp: ").append(timeNow).append("]\n\n")
                  .append("1. Anomaly Vector: Mass Student Record Enumeration (Scraping)\n")
                  .append("Account '").append(actorUsername).append("' accessed multiple student personal files and transcripts in rapid succession without associated instructional enrollment. ")
                  .append("This pattern conforms with automated ID crawling / horizontal privilege traversal.\n\n")
                  .append("2. Risk Assessment (Confidence: ").append(confidenceScore).append("%):\n")
                  .append("Confidentiality exposure under FERPA / academic privacy standards. Threat actor may be assembling student rosters or academic standing metrics.\n\n")
                  .append("3. Threat Classification:\n")
                  .append("MITRE ATT&CK: T1114 (Email / Data Harvesting), T1087.001 (Account Discovery).\n\n")
                  .append("4. Anomaly Context:\n")
                  .append(contextDetails);
            }
            case "RULE-TIM-005" -> {
                sb.append("SENTINEL BEHAVIORAL ANALYSIS [Timestamp: ").append(timeNow).append("]\n\n")
                  .append("1. Anomaly Vector: Off-Hours Gradebook Access\n")
                  .append("Portal mutations logged during the designated campus quiet window (02:00 - 05:00 AM local time). While legitimate late-night grading occasionally occurs, high-privilege operations during this interval carry elevated anomaly weights.\n\n")
                  .append("2. Threat Classification:\n")
                  .append("MITRE ATT&CK: T1078 (Valid Accounts / Unusual Time Variance).\n\n")
                  .append("3. Anomaly Context:\n")
                  .append(contextDetails);
            }
            default -> {
                sb.append("SENTINEL BEHAVIORAL ANALYSIS [Timestamp: ").append(timeNow).append("]\n\n")
                  .append("Heuristic rule ").append(ruleCode).append(" triggered for actor '").append(actorUsername).append("'. ")
                  .append("System noted non-standard interaction signature on resource '").append(targetResource).append("'.\n")
                  .append("Confidence: ").append(confidenceScore).append("%\n\n")
                  .append("Context: ").append(contextDetails);
            }
        }
        return sb.toString();
    }
}

