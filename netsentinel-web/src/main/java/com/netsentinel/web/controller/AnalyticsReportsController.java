package com.netsentinel.web.controller;

import com.netsentinel.web.service.AnalyticsService;
import com.netsentinel.web.service.ReportExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/admin/analytics")
public class AnalyticsReportsController {

    private final AnalyticsService analyticsService;
    private final ReportExportService reportExportService;

    public AnalyticsReportsController(AnalyticsService analyticsService, ReportExportService reportExportService) {
        this.analyticsService = analyticsService;
        this.reportExportService = reportExportService;
    }

    @GetMapping
    public String viewAnalytics(@RequestParam(value = "range", defaultValue = "ALL") String range, Model model) {
        Map<String, Object> metrics = analyticsService.getDashboardMetricsWithFilter(range);
        model.addAttribute("metrics", metrics);
        model.addAttribute("selectedRange", range);
        return "admin/analytics";
    }

    @GetMapping("/export")
    public void exportDefault(HttpServletResponse response) throws IOException {
        byte[] csv = reportExportService.exportIncidentsCsv();
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"netsentinel-incidents-" + System.currentTimeMillis() + ".csv\"");
        response.getOutputStream().write(csv);
        response.getOutputStream().flush();
    }

    @GetMapping("/export/incidents.csv")
    public ResponseEntity<byte[]> exportIncidentsCsv() {
        byte[] bytes = reportExportService.exportIncidentsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"netsentinel-incidents-" + System.currentTimeMillis() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/export/incidents.pdf")
    public ResponseEntity<byte[]> exportIncidentsPdf() {
        byte[] bytes = reportExportService.exportIncidentsPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"netsentinel-incidents-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/export/activity.csv")
    public ResponseEntity<byte[]> exportActivityCsv() {
        byte[] bytes = reportExportService.exportActivityLogsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"netsentinel-audit-trail-" + System.currentTimeMillis() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/export/activity.pdf")
    public ResponseEntity<byte[]> exportActivityPdf() {
        byte[] bytes = reportExportService.exportActivityLogsPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"netsentinel-activity-audit.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/export/health.pdf")
    public ResponseEntity<byte[]> exportHealthPdf() {
        byte[] bytes = reportExportService.exportSecurityHealthPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"netsentinel-security-health.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/export/simulation.pdf")
    public ResponseEntity<byte[]> exportSimulationPdf() {
        byte[] bytes = reportExportService.exportSimulationReportPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"netsentinel-simulation-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
