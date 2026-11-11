package com.netsentinel.web.controller;

import com.netsentinel.web.model.DetectionRuleConfig;
import com.netsentinel.web.repository.DetectionRuleRepository;
import com.netsentinel.web.security.UserSession;
import com.netsentinel.web.service.ActivityLogService;
import com.netsentinel.web.service.PolicyTestingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/rules")
public class DetectionRulesController {

    private final DetectionRuleRepository ruleRepository;
    private final ActivityLogService activityLogService;
    private final PolicyTestingService policyTestingService;

    public DetectionRulesController(DetectionRuleRepository ruleRepository, 
                                    ActivityLogService activityLogService,
                                    PolicyTestingService policyTestingService) {
        this.ruleRepository = ruleRepository;
        this.activityLogService = activityLogService;
        this.policyTestingService = policyTestingService;
    }

    @GetMapping
    public String listRules(Model model) {
        List<DetectionRuleConfig> rules = ruleRepository.findAll();
        model.addAttribute("rules", rules);
        return "admin/rules";
    }

    @PostMapping("/toggle")
    public String toggleRule(@RequestParam("ruleCode") String ruleCode,
                             HttpSession session,
                             HttpServletRequest request) {
        UserSession admin = (UserSession) session.getAttribute("userSession");
        ruleRepository.toggleRule(ruleCode);

        activityLogService.logAllowed(admin.getId(), admin.getUsername(), "ADMIN", "RULE_TOGGLE",
                "RULE", ruleCode, "/admin/rules/toggle", request.getRemoteAddr(),
                "Toggled active state of detection rule " + ruleCode);

        return "redirect:/admin/rules?success=toggled";
    }

    @PostMapping("/update")
    public String updateRule(@RequestParam("ruleCode") String ruleCode,
                             @RequestParam("threshold") Integer threshold,
                             @RequestParam("timeWindowSeconds") Integer timeWindowSeconds,
                             @RequestParam("severity") String severity,
                             @RequestParam(value = "enabled", defaultValue = "false") boolean enabled,
                             HttpSession session,
                             HttpServletRequest request) {
        UserSession admin = (UserSession) session.getAttribute("userSession");
        ruleRepository.updateRule(ruleCode, enabled, threshold, timeWindowSeconds, severity);

        activityLogService.logAllowed(admin.getId(), admin.getUsername(), "ADMIN", "RULE_CONFIG_UPDATE",
                "RULE", ruleCode, "/admin/rules/update", request.getRemoteAddr(),
                "Reconfigured detection parameters for " + ruleCode + " (Threshold=" + threshold + ", Window=" + timeWindowSeconds + "s, Severity=" + severity + ")");

        return "redirect:/admin/rules?success=updated";
    }

    @PostMapping("/what-if")
    public String runWhatIfTest(@RequestParam("ruleCode") String ruleCode,
                                @RequestParam("threshold") Integer threshold,
                                @RequestParam("timeWindowSeconds") Integer timeWindowSeconds,
                                @RequestParam("severity") String severity,
                                @RequestParam("activityPattern") String activityPattern,
                                Model model) {
        Map<String, Object> testResult = policyTestingService.runWhatIfPolicyTest(
                ruleCode, threshold, timeWindowSeconds, severity, activityPattern);
        model.addAttribute("whatIfResult", testResult);
        model.addAttribute("rules", ruleRepository.findAll());
        return "admin/rules";
    }

    @PostMapping("/api/what-if")
    @ResponseBody
    public Map<String, Object> runWhatIfTestApi(@RequestParam("ruleCode") String ruleCode,
                                                @RequestParam(value = "threshold", defaultValue = "20") Integer threshold,
                                                @RequestParam(value = "timeWindowSeconds", defaultValue = "120") Integer timeWindowSeconds,
                                                @RequestParam(value = "severity", defaultValue = "HIGH") String severity,
                                                @RequestParam(value = "activityPattern", defaultValue = "BURST_BATCH_GRADING") String activityPattern) {
        return policyTestingService.runWhatIfPolicyTest(ruleCode, threshold, timeWindowSeconds, severity, activityPattern);
    }
}
