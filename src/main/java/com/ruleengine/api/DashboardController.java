package com.ruleengine.api;

import com.ruleengine.api.dto.DashboardMetrics;
import com.ruleengine.api.dto.ExecutionSummary;
import com.ruleengine.api.dto.RulePerformance;
import com.ruleengine.api.dto.RuleSummary;
import com.ruleengine.audit.AuditLogger;
import com.ruleengine.cache.RuleCache;
import com.ruleengine.model.Rule;
import com.ruleengine.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard Controller
 *
 * Provides endpoints for the Admin Dashboard UI:
 * - Overall metrics and statistics
 * - Recent execution history
 * - Top performing rules
 * - Search and filtering
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private AuditLogger auditLogger;

    @Autowired
    private RuleCache ruleCache;

    /**
     * Get dashboard metrics
     *
     * Returns overall statistics including:
     * - Total rules, active rules
     * - Execution statistics
     * - Cache performance
     * - Executions by hour and status
     */
    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetrics> getMetrics() {
        List<Rule> allRules = ruleService.getAllRules();

        long totalRules = allRules.size();
        long activeRules = allRules.stream().filter(r -> r.getEnabled() != null && r.getEnabled()).count();
        long disabledRules = totalRules - activeRules;

        long totalExecutions = auditLogger.getTotalExecutions();
        long totalMatches = auditLogger.getTotalMatches();
        long totalErrors = auditLogger.getTotalErrors();
        double avgExecutionTime = auditLogger.getAverageExecutionTime();
        double successRate = auditLogger.getSuccessRate();
        double matchRate = totalExecutions > 0 ? (totalMatches * 100.0) / totalExecutions : 0.0;

        Map<String, Long> cacheStats = ruleCache.getStatistics();
        long cacheHits = cacheStats.getOrDefault("hits", 0L);
        long cacheMisses = cacheStats.getOrDefault("misses", 0L);
        double cacheHitRate = (cacheHits + cacheMisses) > 0
            ? (cacheHits * 100.0) / (cacheHits + cacheMisses)
            : 0.0;

        Map<String, Long> executionsByHour = auditLogger.getExecutionsByHour();
        Map<String, Long> executionsByStatus = auditLogger.getExecutionsByStatus();

        DashboardMetrics metrics = DashboardMetrics.builder()
            .totalRules(totalRules)
            .activeRules(activeRules)
            .disabledRules(disabledRules)
            .totalExecutions(totalExecutions)
            .totalMatches(totalMatches)
            .totalErrors(totalErrors)
            .avgExecutionTimeMs(avgExecutionTime)
            .successRate(successRate)
            .matchRate(matchRate)
            .cacheHits(cacheHits)
            .cacheMisses(cacheMisses)
            .cacheHitRate(cacheHitRate)
            .executionsByHour(executionsByHour)
            .executionsByStatus(executionsByStatus)
            .build();

        return ResponseEntity.ok(metrics);
    }

    /**
     * Get recent executions
     *
     * Returns the most recent rule executions with details
     */
    @GetMapping("/recent-executions")
    public ResponseEntity<List<ExecutionSummary>> getRecentExecutions(
            @RequestParam(defaultValue = "50") int limit) {

        List<ExecutionSummary> executions = auditLogger.getRecentExecutions(limit).stream()
            .map(entry -> ExecutionSummary.builder()
                .ruleId(entry.ruleId())
                .ruleName(entry.ruleName())
                .timestamp(entry.timestamp())
                .matched(entry.matched() != null && entry.matched())
                .status(entry.status())
                .executionTimeMs(entry.executionTimeMs() != null ? entry.executionTimeMs() : 0)
                .build())
            .collect(Collectors.toList());

        return ResponseEntity.ok(executions);
    }

    /**
     * Get top rules by execution count
     *
     * Returns rules ordered by how frequently they are executed
     */
    @GetMapping("/top-rules")
    public ResponseEntity<List<RulePerformance>> getTopRules(
            @RequestParam(defaultValue = "10") int limit) {

        List<Rule> allRules = ruleService.getAllRules();

        List<RulePerformance> topRules = allRules.stream()
            .map(rule -> {
                AuditLogger.RuleExecutionStats stats = auditLogger.getRuleExecutionStats(rule.getId());
                return RulePerformance.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .executionCount(stats.executionCount())
                    .matchCount(stats.matchCount())
                    .avgExecutionTimeMs(stats.avgExecutionTimeMs())
                    .matchRate(stats.matchRate())
                    .errorCount(stats.errorCount())
                    .build();
            })
            .filter(perf -> perf.getExecutionCount() > 0)
            .sorted((a, b) -> Long.compare(b.getExecutionCount(), a.getExecutionCount()))
            .limit(limit)
            .collect(Collectors.toList());

        return ResponseEntity.ok(topRules);
    }

    /**
     * Search rules
     *
     * Search rules by name, description, or group
     * Supports pagination
     */
    @GetMapping("/rules/search")
    public ResponseEntity<Page<RuleSummary>> searchRules(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) Boolean enabled,
            Pageable pageable) {

        List<Rule> allRules = ruleService.getAllRules();

        // Filter rules based on search criteria
        List<Rule> filteredRules = allRules.stream()
            .filter(rule -> {
                if (query != null && !query.isBlank()) {
                    String lowerQuery = query.toLowerCase();
                    return rule.getName().toLowerCase().contains(lowerQuery) ||
                           (rule.getDescription() != null && rule.getDescription().toLowerCase().contains(lowerQuery)) ||
                           rule.getId().toLowerCase().contains(lowerQuery);
                }
                return true;
            })
            .filter(rule -> group == null || group.equals(rule.getRuleGroup()))
            .filter(rule -> enabled == null || enabled.equals(rule.getEnabled()))
            .toList();

        // Convert to RuleSummary with execution stats
        List<RuleSummary> summaries = filteredRules.stream()
            .map(rule -> {
                AuditLogger.RuleExecutionStats stats = auditLogger.getRuleExecutionStats(rule.getId());
                return RuleSummary.builder()
                    .id(rule.getId())
                    .name(rule.getName())
                    .description(rule.getDescription())
                    .priority(rule.getPriority())
                    .salience(rule.getSalience())
                    .enabled(rule.getEnabled())
                    .ruleGroup(rule.getRuleGroup())
                    .version(rule.getVersion())
                    .createdAt(Instant.now()) // Would be from metadata
                    .updatedAt(Instant.now()) // Would be from metadata
                    .executionCount(stats.executionCount())
                    .avgExecutionTimeMs(stats.avgExecutionTimeMs())
                    .build();
            })
            .toList();

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), summaries.size());
        List<RuleSummary> pageContent = summaries.subList(start, Math.min(end, summaries.size()));

        Page<RuleSummary> page = new PageImpl<>(pageContent, pageable, summaries.size());

        return ResponseEntity.ok(page);
    }

    /**
     * Get rule execution history
     *
     * Returns execution history for a specific rule
     */
    @GetMapping("/rules/{id}/execution-history")
    public ResponseEntity<List<ExecutionSummary>> getRuleExecutionHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") int limit) {

        List<ExecutionSummary> history = auditLogger.getAuditLog(id, limit).stream()
            .filter(entry -> entry.eventType() == AuditLogger.AuditEventType.RULE_EXECUTION)
            .map(entry -> ExecutionSummary.builder()
                .ruleId(entry.ruleId())
                .ruleName(entry.ruleName())
                .timestamp(entry.timestamp())
                .matched(entry.matched() != null && entry.matched())
                .status(entry.status())
                .executionTimeMs(entry.executionTimeMs() != null ? entry.executionTimeMs() : 0)
                .build())
            .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    /**
     * Get rule performance metrics
     *
     * Returns detailed performance metrics for a specific rule
     */
    @GetMapping("/rules/{id}/performance")
    public ResponseEntity<RulePerformance> getRulePerformance(@PathVariable String id) {
        Rule rule = ruleService.getRule(id);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }

        AuditLogger.RuleExecutionStats stats = auditLogger.getRuleExecutionStats(id);

        RulePerformance performance = RulePerformance.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .executionCount(stats.executionCount())
            .matchCount(stats.matchCount())
            .avgExecutionTimeMs(stats.avgExecutionTimeMs())
            .matchRate(stats.matchRate())
            .errorCount(stats.errorCount())
            .build();

        return ResponseEntity.ok(performance);
    }

    /**
     * Get all rule groups
     *
     * Returns list of unique rule groups
     */
    @GetMapping("/rule-groups")
    public ResponseEntity<List<String>> getRuleGroups() {
        List<String> groups = ruleService.getAllRules().stream()
            .map(Rule::getRuleGroup)
            .filter(group -> group != null && !group.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        return ResponseEntity.ok(groups);
    }

    /**
     * Get cache statistics
     *
     * Returns detailed cache performance metrics
     */
    @GetMapping("/cache/statistics")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Long> stats = ruleCache.getStatistics();

        long hits = stats.getOrDefault("hits", 0L);
        long misses = stats.getOrDefault("misses", 0L);
        long total = hits + misses;
        double hitRate = total > 0 ? (hits * 100.0) / total : 0.0;

        Map<String, Object> response = new HashMap<>();
        response.put("hits", hits);
        response.put("misses", misses);
        response.put("totalRequests", total);
        response.put("hitRate", hitRate);
        response.put("size", stats.getOrDefault("size", 0L));
        response.put("evictions", stats.getOrDefault("evictions", 0L));

        return ResponseEntity.ok(response);
    }
}
