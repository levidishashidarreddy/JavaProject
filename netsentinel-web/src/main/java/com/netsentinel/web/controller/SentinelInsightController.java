package com.netsentinel.web.controller;

import com.netsentinel.web.engine.insight.ExplainableDossier;
import com.netsentinel.web.engine.insight.SentinelInsightService;
import com.netsentinel.web.model.Incident;
import com.netsentinel.web.repository.DetectionRuleRepository;
import com.netsentinel.web.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/insight")
public class SentinelInsightController {

    private final IncidentService incidentService;
    private final DetectionRuleRepository ruleRepository;
    private final SentinelInsightService insightService;

    public SentinelInsightController(IncidentService incidentService, 
                                     DetectionRuleRepository ruleRepository,
                                     SentinelInsightService insightService) {
        this.incidentService = incidentService;
        this.ruleRepository = ruleRepository;
        this.insightService = insightService;
    }

    @GetMapping
    public String viewInsight(@RequestParam(value = "incidentId", required = false) Long incidentId, Model model) {
        List<Incident> recentIncidents = incidentService.getRecentIncidents(10);
        model.addAttribute("recentIncidents", recentIncidents);
        model.addAttribute("rules", ruleRepository.findAll());
        model.addAttribute("stats", incidentService.getStatsSummary());

        // Resolve target incident for explainable intelligence dossier
        Long targetId = incidentId;
        if (targetId == null && !recentIncidents.isEmpty()) {
            targetId = recentIncidents.get(0).getId();
        }

        if (targetId != null) {
            Optional<ExplainableDossier> dossierOpt = insightService.generateDossier(targetId);
            dossierOpt.ifPresent(dossier -> model.addAttribute("dossier", dossier));
        }

        return "admin/insight";
    }

    @GetMapping("/api/dossier/{incidentId}")
    @ResponseBody
    public ResponseEntity<ExplainableDossier> getDossierApi(@PathVariable("incidentId") Long incidentId) {
        return insightService.generateDossier(incidentId)
                .map(org.springframework.http.ResponseEntity::ok)
                .orElse(org.springframework.http.ResponseEntity.notFound().build());
    }
}
