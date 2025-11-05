package com.ruleengine.demo;

import com.ruleengine.model.Rule;
import com.ruleengine.model.RuleContext;
import com.ruleengine.model.RuleResult;
import com.ruleengine.service.RuleService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Demo Controller - Serves the demo UI and provides demo-specific endpoints
 */
@Controller
@RequestMapping("/demo")
@CrossOrigin(origins = "*")
public class DemoController {

    private final RuleService ruleService;
    private final DemoUseCaseService useCaseService;

    public DemoController(RuleService ruleService, DemoUseCaseService useCaseService) {
        this.ruleService = ruleService;
        this.useCaseService = useCaseService;
    }

    /**
     * Serve the demo UI
     */
    @GetMapping
    public String demo() {
        return "demo";
    }

    /**
     * Get all demo use cases
     */
    @GetMapping("/use-cases")
    @ResponseBody
    public List<DemoUseCase> getUseCases() {
        return useCaseService.getAllUseCases();
    }

    /**
     * Get a specific use case
     */
    @GetMapping("/use-cases/{id}")
    @ResponseBody
    public DemoUseCase getUseCase(@PathVariable String id) {
        return useCaseService.getUseCase(id);
    }

    /**
     * Load rules for a use case
     */
    @PostMapping("/use-cases/{id}/load")
    @ResponseBody
    public Map<String, Object> loadUseCase(@PathVariable String id) {
        List<Rule> rules = useCaseService.loadUseCaseRules(id);
        return Map.of(
            "success", true,
            "message", "Loaded " + rules.size() + " rules for use case: " + id,
            "rules", rules
        );
    }

    /**
     * Execute a demo scenario
     */
    @PostMapping("/use-cases/{id}/execute")
    @ResponseBody
    public Map<String, Object> executeScenario(
            @PathVariable String id,
            @RequestBody Map<String, Object> scenario) {

        DemoUseCase useCase = useCaseService.getUseCase(id);

        // Create context from scenario facts
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> facts = (Map<String, Map<String, Object>>) scenario.get("facts");
        RuleContext context = RuleContext.withFacts(facts);

        // Execute rules for the use case group
        List<RuleResult> results = ruleService.executeRuleGroup(useCase.getGroup(), context);

        return Map.of(
            "success", true,
            "useCase", useCase.getName(),
            "scenario", scenario.get("name"),
            "results", results,
            "summary", generateSummary(results)
        );
    }

    /**
     * Reset demo data
     */
    @PostMapping("/reset")
    @ResponseBody
    public Map<String, Object> resetDemo() {
        // Clear all rules
        ruleService.getAllRules().forEach(rule -> ruleService.deleteRule(rule.getId()));

        return Map.of(
            "success", true,
            "message", "Demo data reset successfully"
        );
    }

    private Map<String, Object> generateSummary(List<RuleResult> results) {
        long executed = results.stream().filter(RuleResult::executed).count();
        long matched = results.stream().filter(RuleResult::matched).count();
        long successful = results.stream().filter(r -> r.executed() && !r.error()).count();

        return Map.of(
            "totalRules", results.size(),
            "executed", executed,
            "matched", matched,
            "successful", successful,
            "averageExecutionTime", results.stream()
                .mapToLong(RuleResult::executionTimeMs)
                .average()
                .orElse(0.0)
        );
    }
}
