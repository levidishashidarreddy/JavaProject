package com.netsentinel.web.controller;

import com.netsentinel.web.model.SimulationResponse;
import com.netsentinel.web.model.SimulationRun;
import com.netsentinel.web.service.SimulationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/simulation")
public class SimulationLabController {

    private final SimulationService simulationService;

    public SimulationLabController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping
    public String viewSimulationLab(@RequestParam(value = "simType", required = false) String simType,
                                    @RequestParam(value = "actor", required = false) String actor,
                                    Model model) {
        List<SimulationRun> recentRuns = simulationService.getRecentSimulationRuns(10);
        model.addAttribute("recentRuns", recentRuns);
        return "admin/simulation";
    }

    @PostMapping("/run")
    public String runSimulation(@RequestParam("simulationType") String simulationType,
                                @RequestParam(value = "actor", defaultValue = "student101") String actor,
                                HttpServletRequest request,
                                Model model) {
        SimulationResponse result = simulationService.runSimulation(simulationType, actor, request.getRemoteAddr());
        model.addAttribute("simResult", result);
        model.addAttribute("recentRuns", simulationService.getRecentSimulationRuns(10));
        return "admin/simulation";
    }

    @PostMapping("/api/run")
    @ResponseBody
    public SimulationResponse runSimulationApi(@RequestParam("simulationType") String simulationType,
                                               @RequestParam(value = "actor", defaultValue = "student101") String actor,
                                               HttpServletRequest request) {
        return simulationService.runSimulation(simulationType, actor, request.getRemoteAddr());
    }

    @PostMapping("/reset")
    public String resetDemo() {
        simulationService.resetDemoData();
        return "redirect:/admin/simulation?success=reset_completed";
    }

    @PostMapping("/api/reset")
    @ResponseBody
    public Map<String, Object> resetDemoApi() {
        simulationService.resetDemoData();
        return Map.of("success", true, "message", "Simulation environment and demo state reset successfully.");
    }
}
