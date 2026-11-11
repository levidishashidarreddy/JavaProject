package com.netsentinel.web.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.netsentinel.web.model.ActivityLog;
import com.netsentinel.web.model.Incident;
import com.netsentinel.web.model.SimulationRun;
import com.netsentinel.web.repository.ActivityLogRepository;
import com.netsentinel.web.repository.IncidentRepository;
import com.netsentinel.web.repository.SimulationRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for generating comprehensive CSV and PDF reports.
 * 
 * Supports:
 * 1. Incident Report (CSV / PDF)
 * 2. Activity Audit Report (CSV / PDF)
 * 3. Security Health Summary Report (CSV / PDF)
 * 4. Simulation Lab Report (CSV / PDF)
 * 
 * Demonstrates:
 * - Java I/O Streams (ByteArrayOutputStream, PrintWriter)
 * - Professional PDF compilation using OpenPDF
 * - NetSentinel cyber-defense branding with deep navy (#101827) and teal (#20A99A)
 */
@Service
public class ReportExportService {

    private final IncidentRepository incidentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final SimulationRepository simulationRepository;
    private final AnalyticsService analyticsService;

    // Palette Colors
    private static final Color NAVY_DARK = new Color(16, 24, 39);     // #101827
    private static final Color TEAL_ACCENT = new Color(32, 169, 154); // #20A99A
    private static final Color ROW_ALT = new Color(245, 247, 250);     // #F5F7FA
    private static final Color TEXT_DARK = new Color(30, 41, 59);

    public ReportExportService(IncidentRepository incidentRepository,
                               ActivityLogRepository activityLogRepository,
                               SimulationRepository simulationRepository,
                               AnalyticsService analyticsService) {
        this.incidentRepository = incidentRepository;
        this.activityLogRepository = activityLogRepository;
        this.simulationRepository = simulationRepository;
        this.analyticsService = analyticsService;
    }

    // ==========================================
    // 1. INCIDENT REPORT (CSV & PDF)
    // ==========================================

    public byte[] exportIncidentsCsv() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos)) {
            writer.println("Incident Number,Title,Rule Code,Severity,Category,Status,Actor,Resource,Confidence,Created At");
            List<Incident> incidents = incidentRepository.findRecent(200);
            for (Incident inc : incidents) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\"%n",
                        safe(inc.getIncidentNumber()),
                        safe(inc.getTitle()),
                        safe(inc.getRuleCode()),
                        safe(inc.getSeverity()),
                        safe(inc.getCategory()),
                        safe(inc.getStatus()),
                        safe(inc.getActorUsername()),
                        safe(inc.getTargetResource()),
                        inc.getConfidenceScore() != null ? inc.getConfidenceScore() : 85,
                        inc.getCreatedAt() != null ? inc.getCreatedAt().toString() : "");
            }
            writer.flush();
        }
        return baos.toByteArray();
    }

    public byte[] exportIncidentsPdf() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            addBrandedHeader(document, "SECURITY INCIDENTS REPORT", "Comprehensive audit of anomalous events & investigation statuses");

            // Summary Table
            Map<String, Object> metrics = analyticsService.getDashboardMetrics();
            PdfPTable summary = new PdfPTable(4);
            summary.setWidthPercentage(100);
            summary.setSpacingAfter(15);

            addStatBox(summary, "Total Incidents", String.valueOf(metrics.get("totalIncidents")));
            addStatBox(summary, "High/Critical Severity", String.valueOf(metrics.get("highCriticalIncidents")));
            addStatBox(summary, "Open Incidents", String.valueOf(metrics.get("openIncidents")));
            addStatBox(summary, "Campus Health Score", metrics.get("healthScore") + " / 100");
            document.add(summary);

            // Incidents Data Table
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{16, 28, 12, 12, 12, 14, 16});

            addTableHeader(table, "Incident #");
            addTableHeader(table, "Title");
            addTableHeader(table, "Rule Code");
            addTableHeader(table, "Severity");
            addTableHeader(table, "Status");
            addTableHeader(table, "Actor");
            addTableHeader(table, "Timestamp");

            List<Incident> incidents = incidentRepository.findRecent(50);
            boolean alt = false;
            for (Incident inc : incidents) {
                Color bg = alt ? ROW_ALT : Color.WHITE;
                addTableCell(table, inc.getIncidentNumber(), bg, true);
                addTableCell(table, inc.getTitle(), bg, false);
                addTableCell(table, inc.getRuleCode(), bg, false);
                addTableCell(table, inc.getSeverity(), bg, false);
                addTableCell(table, inc.getStatus(), bg, false);
                addTableCell(table, inc.getActorUsername(), bg, false);
                addTableCell(table, inc.getCreatedAt() != null ? inc.getCreatedAt().toString().replace("T", " ").substring(0, 16) : "-", bg, false);
                alt = !alt;
            }
            document.add(table);

            addBrandedFooter(document);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating incidents PDF report", e);
        }
        return baos.toByteArray();
    }

    // ==========================================
    // 2. ACTIVITY AUDIT REPORT (CSV & PDF)
    // ==========================================

    public byte[] exportActivityLogsCsv() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos)) {
            writer.println("Event ID,Actor,Role,Action,Target Type,Target ID,Outcome,IP Address,Timestamp");
            List<ActivityLog> logs = activityLogRepository.findRecent(500);
            for (ActivityLog log : logs) {
                writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        log.getId(),
                        safe(log.getActorUsername()),
                        safe(log.getActorRole()),
                        safe(log.getAction()),
                        safe(log.getTargetType()),
                        safe(log.getTargetId()),
                        safe(log.getOutcome()),
                        safe(log.getIpAddress()),
                        log.getTimestamp() != null ? log.getTimestamp().toString() : "");
            }
            writer.flush();
        }
        return baos.toByteArray();
    }

    public byte[] exportActivityLogsPdf() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            addBrandedHeader(document, "ACTIVITY AUDIT TRAIL REPORT", "Immutable log of academic portal actions, authorizations and denials");

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{8, 14, 12, 22, 24, 12, 16});

            addTableHeader(table, "ID");
            addTableHeader(table, "Actor");
            addTableHeader(table, "Role");
            addTableHeader(table, "Action");
            addTableHeader(table, "Target");
            addTableHeader(table, "Outcome");
            addTableHeader(table, "Timestamp");

            List<ActivityLog> logs = activityLogRepository.findRecent(60);
            boolean alt = false;
            for (ActivityLog log : logs) {
                Color bg = alt ? ROW_ALT : Color.WHITE;
                addTableCell(table, String.valueOf(log.getId()), bg, true);
                addTableCell(table, log.getActorUsername(), bg, false);
                addTableCell(table, log.getActorRole(), bg, false);
                addTableCell(table, log.getAction(), bg, false);
                addTableCell(table, log.getTargetResource(), bg, false);
                addTableCell(table, log.getOutcome(), bg, false);
                addTableCell(table, log.getTimestamp() != null ? log.getTimestamp().toString().replace("T", " ").substring(0, 16) : "-", bg, false);
                alt = !alt;
            }
            document.add(table);

            addBrandedFooter(document);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating activity audit PDF report", e);
        }
        return baos.toByteArray();
    }

    // ==========================================
    // 3. SECURITY HEALTH REPORT (PDF)
    // ==========================================

    public byte[] exportSecurityHealthPdf() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 25, 25, 25, 25);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            addBrandedHeader(document, "CAMPUS SECURITY HEALTH ASSESSMENT", "Executive threat posture telemetry and health score computation");

            Map<String, Object> metrics = analyticsService.getDashboardMetrics();
            Map<String, Object> health = (Map<String, Object>) metrics.get("securityHealth");

            PdfPTable scoreTable = new PdfPTable(2);
            scoreTable.setWidthPercentage(100);
            scoreTable.setSpacingAfter(15);
            addStatBox(scoreTable, "Security Health Score", health.get("score") + " / 100 (" + health.get("status") + ")");
            addStatBox(scoreTable, "Active Threat Index", metrics.get("threatIndex") + " / 100");
            document.add(scoreTable);

            // Formula Explanation Section
            Font secFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NAVY_DARK);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_DARK);

            Paragraph formulaP = new Paragraph("Health Computation Formula:", secFont);
            formulaP.setSpacingAfter(4);
            document.add(formulaP);

            Paragraph formulaBody = new Paragraph(
                    "Score = Base (100) - [Open High/Critical × 12] - [Open Medium/Low × 6] - [Under Investigation × 4] + [Resolved × 3]. Capped between 0 and 100.",
                    bodyFont
            );
            formulaBody.setSpacingAfter(15);
            document.add(formulaBody);

            // Active Rules Breakdown
            Paragraph rulesTitle = new Paragraph("Top Triggered Behavioral Detection Rules:", secFont);
            rulesTitle.setSpacingAfter(6);
            document.add(rulesTitle);

            PdfPTable rulesTable = new PdfPTable(4);
            rulesTable.setWidthPercentage(100);
            rulesTable.setWidths(new float[]{20, 45, 20, 15});

            addTableHeader(rulesTable, "Rule Code");
            addTableHeader(rulesTable, "Rule Description");
            addTableHeader(rulesTable, "Severity");
            addTableHeader(rulesTable, "Trigger Count");

            List<Map<String, Object>> topRules = (List<Map<String, Object>>) metrics.get("topTriggeredRules");
            if (topRules != null) {
                for (Map<String, Object> r : topRules) {
                    addTableCell(rulesTable, (String) r.get("rule_code"), Color.WHITE, true);
                    addTableCell(rulesTable, (String) r.get("title"), Color.WHITE, false);
                    addTableCell(rulesTable, (String) r.get("severity"), Color.WHITE, false);
                    addTableCell(rulesTable, String.valueOf(r.get("count")), Color.WHITE, false);
                }
            }
            document.add(rulesTable);

            addBrandedFooter(document);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating security health PDF report", e);
        }
        return baos.toByteArray();
    }

    // ==========================================
    // 4. SIMULATION LAB REPORT (PDF)
    // ==========================================

    public byte[] exportSimulationReportPdf() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 25, 25, 25, 25);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            addBrandedHeader(document, "ATTACK SIMULATION LAB AUDIT REPORT", "Verification of threat scenario reproducibility & detection accuracy");

            List<SimulationRun> runs = simulationRepository.findRecentRuns(30);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{10, 30, 15, 25, 20});

            addTableHeader(table, "Run ID");
            addTableHeader(table, "Threat Scenario");
            addTableHeader(table, "Detected?");
            addTableHeader(table, "Outcome Summary");
            addTableHeader(table, "Execution Time");

            for (SimulationRun r : runs) {
                addTableCell(table, String.valueOf(r.getId()), Color.WHITE, true);
                addTableCell(table, r.getSimulationType(), Color.WHITE, false);
                addTableCell(table, r.isDetected() ? "YES (DETECTED)" : "NO", Color.WHITE, false);
                addTableCell(table, r.getSummary() != null ? r.getSummary() : "Execution completed", Color.WHITE, false);
                addTableCell(table, r.getRunAt() != null ? r.getRunAt().toString().replace("T", " ").substring(0, 16) : "-", Color.WHITE, false);
            }
            document.add(table);

            addBrandedFooter(document);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating simulation PDF report", e);
        }
        return baos.toByteArray();
    }

    // ==========================================
    // PDF Styling & Branding Helpers
    // ==========================================

    private void addBrandedHeader(Document doc, String title, String subtitle) throws DocumentException {
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, TEAL_ACCENT);
        Font subBrandFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, NAVY_DARK);
        Font descFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);

        Paragraph brand = new Paragraph("NETSENTINEL", brandFont);
        Paragraph subBrand = new Paragraph("Intelligent Campus Portal Security & Incident Analyzer", subBrandFont);
        subBrand.setSpacingAfter(8);

        Paragraph reportTitle = new Paragraph(title, titleFont);
        Paragraph desc = new Paragraph(subtitle + " | Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), descFont);
        desc.setSpacingAfter(14);

        doc.add(brand);
        doc.add(subBrand);
        doc.add(reportTitle);
        doc.add(desc);
    }

    private void addBrandedFooter(Document doc) throws DocumentException {
        Font footFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Paragraph foot = new Paragraph("\nNetSentinel Cybersecurity Operations Center • Academic Demonstration Prototype • Confidential", footFont);
        foot.setAlignment(Element.ALIGN_CENTER);
        doc.add(foot);
    }

    private void addStatBox(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(ROW_ALT);
        cell.setPadding(8);
        cell.setBorderColor(new Color(226, 232, 240));

        Font lblFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, NAVY_DARK);

        cell.addElement(new Paragraph(label, lblFont));
        cell.addElement(new Paragraph(value, valFont));
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        cell.setBackgroundColor(NAVY_DARK);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Color bg, boolean bold) {
        Font font = FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 8, TEXT_DARK);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }

    private String safe(String s) {
        return s != null ? s.replace("\"", "\"\"") : "";
    }
}

