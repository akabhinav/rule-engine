package com.ruleengine.api;

import com.ruleengine.cache.RuleCache;
import com.ruleengine.core.RuleEngine;
import com.ruleengine.model.Rule;
import com.ruleengine.model.RuleContext;
import com.ruleengine.model.RuleResult;
import com.ruleengine.service.RuleService;
import com.ruleengine.websocket.RuleExecutionNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Rule REST API
 *
 * Endpoints for rule management and execution
 */
@RestController
@RequestMapping("/api/rules")
@CrossOrigin(origins = "*")
public class RuleController {

    private final RuleService ruleService;

    @Autowired(required = false)
    private RuleExecutionNotifier executionNotifier;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * Create a new rule
     */
    @PostMapping
    public ResponseEntity<Rule> createRule(@RequestBody Rule rule) {
        Rule created = ruleService.createRule(rule);

        // Notify about rule creation
        if (executionNotifier != null) {
            executionNotifier.notifyRuleChange("CREATED", created.getId(), created.getName());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Create rule from JSON string
     */
    @PostMapping("/import/json")
    public ResponseEntity<Rule> createRuleFromJson(@RequestBody String json) {
        Rule created = ruleService.createRuleFromJson(json);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get rule by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Rule> getRule(@PathVariable String id) {
        return ruleService.getRule(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all rules
     */
    @GetMapping
    public ResponseEntity<List<Rule>> getAllRules(
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String tenant) {

        if (group != null) {
            return ResponseEntity.ok(ruleService.getRulesByGroup(group));
        }

        if (tenant != null) {
            return ResponseEntity.ok(ruleService.getRulesByTenant(tenant));
        }

        return ResponseEntity.ok(ruleService.getAllRules());
    }

    /**
     * Update rule
     */
    @PutMapping("/{id}")
    public ResponseEntity<Rule> updateRule(@PathVariable String id, @RequestBody Rule rule) {
        try {
            Rule updated = ruleService.updateRule(id, rule);

            // Notify about rule update
            if (executionNotifier != null) {
                executionNotifier.notifyRuleChange("UPDATED", updated.getId(), updated.getName());
            }

            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete rule
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable String id) {
        Rule rule = ruleService.getRule(id).orElse(null);
        ruleService.deleteRule(id);

        // Notify about rule deletion
        if (executionNotifier != null && rule != null) {
            executionNotifier.notifyRuleChange("DELETED", id, rule.getName());
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Execute a single rule
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<RuleResult> executeRule(
            @PathVariable String id,
            @RequestBody Map<String, Map<String, Object>> facts) {

        RuleContext context = RuleContext.withFacts(facts);
        RuleResult result = ruleService.executeRule(id, context);
        return ResponseEntity.ok(result);
    }

    /**
     * Execute rule group
     */
    @PostMapping("/groups/{group}/execute")
    public ResponseEntity<List<RuleResult>> executeRuleGroup(
            @PathVariable String group,
            @RequestBody Map<String, Map<String, Object>> facts) {

        RuleContext context = RuleContext.withFacts(facts);
        List<RuleResult> results = ruleService.executeRuleGroup(group, context);
        return ResponseEntity.ok(results);
    }

    /**
     * Execute all rules for tenant
     */
    @PostMapping("/tenants/{tenant}/execute")
    public ResponseEntity<List<RuleResult>> executeForTenant(
            @PathVariable String tenant,
            @RequestBody Map<String, Map<String, Object>> facts) {

        RuleContext context = RuleContext.withFacts(facts);
        context.setTenant(tenant);
        List<RuleResult> results = ruleService.executeForTenant(tenant, context);
        return ResponseEntity.ok(results);
    }

    /**
     * Test rule without saving
     */
    @PostMapping("/test")
    public ResponseEntity<RuleResult> testRule(
            @RequestBody TestRuleRequest request) {

        RuleContext context = RuleContext.withFacts(request.facts());
        RuleResult result = ruleService.testRule(request.rule(), context);
        return ResponseEntity.ok(result);
    }

    /**
     * Import multiple rules
     */
    @PostMapping("/import/batch")
    public ResponseEntity<List<Rule>> importRules(@RequestBody String json) {
        List<Rule> rules = ruleService.importRules(json);
        return ResponseEntity.status(HttpStatus.CREATED).body(rules);
    }

    /**
     * Export rule to JSON
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<String> exportRule(@PathVariable String id) {
        try {
            String json = ruleService.exportRule(id);
            return ResponseEntity.ok(json);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get engine statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<RuleEngine.EngineStatistics> getStatistics() {
        return ResponseEntity.ok(ruleService.getStatistics());
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/cache/statistics")
    public ResponseEntity<RuleCache.CacheStatistics> getCacheStatistics() {
        return ResponseEntity.ok(ruleService.getCacheStatistics());
    }

    /**
     * Clear cache
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Void> clearCache() {
        ruleService.clearCache();
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        var stats = ruleService.getStatistics();
        var cacheStats = ruleService.getCacheStatistics();

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "totalRules", ruleService.getAllRules().size(),
                "totalExecutions", stats.totalExecutions(),
                "cacheHitRate", cacheStats.hitRate()
        ));
    }

    record TestRuleRequest(Rule rule, Map<String, Map<String, Object>> facts) {}
}
