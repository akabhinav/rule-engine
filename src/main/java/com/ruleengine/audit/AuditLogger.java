package com.ruleengine.audit;

import com.ruleengine.model.Rule;
import com.ruleengine.model.RuleContext;
import com.ruleengine.model.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Audit Logger
 *
 * Comprehensive audit logging for rule execution
 * Tracks:
 * - Rule executions
 * - Condition evaluations
 * - Action executions
 * - Performance metrics
 * - Changes and updates
 */
@Component
public class AuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

    private final Map<String, List<AuditEntry>> auditLog;
    private final boolean enabled;

    public AuditLogger() {
        this.auditLog = new ConcurrentHashMap<>();
        this.enabled = true; // Could be configured
    }

    /**
     * Log rule execution
     */
    public void logExecution(Rule rule, RuleContext context, RuleResult result) {
        if (!enabled) {
            return;
        }

        AuditEntry entry = AuditEntry.builder()
                .timestamp(Instant.now())
                .eventType(AuditEventType.RULE_EXECUTION)
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .contextId(context.getContextId())
                .matched(result.isMatched())
                .executionTimeMs(result.getExecutionTimeMs())
                .status(result.getStatus().name())
                .metadata(Map.of(
                        "priority", rule.getPriority(),
                        "salience", rule.getSalience(),
                        "tenant", context.getTenant() != null ? context.getTenant() : "none"
                ))
                .build();

        logEntry(entry);

        logger.debug("Audit: Rule {} executed - matched={}, time={}ms",
                rule.getName(), result.isMatched(), result.getExecutionTimeMs());
    }

    /**
     * Log rule creation
     */
    public void logRuleCreated(Rule rule) {
        if (!enabled) {
            return;
        }

        AuditEntry entry = AuditEntry.builder()
                .timestamp(Instant.now())
                .eventType(AuditEventType.RULE_CREATED)
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .metadata(Map.of(
                        "version", rule.getVersion() != null ? rule.getVersion() : 0,
                        "group", rule.getRuleGroup() != null ? rule.getRuleGroup() : "default"
                ))
                .build();

        logEntry(entry);
        logger.info("Audit: Rule created - {}", rule.getName());
    }

    /**
     * Log rule update
     */
    public void logRuleUpdated(Rule rule) {
        if (!enabled) {
            return;
        }

        AuditEntry entry = AuditEntry.builder()
                .timestamp(Instant.now())
                .eventType(AuditEventType.RULE_UPDATED)
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .metadata(Map.of(
                        "version", rule.getVersion() != null ? rule.getVersion() : 0
                ))
                .build();

        logEntry(entry);
        logger.info("Audit: Rule updated - {}", rule.getName());
    }

    /**
     * Log rule deletion
     */
    public void logRuleDeleted(String ruleId, String ruleName) {
        if (!enabled) {
            return;
        }

        AuditEntry entry = AuditEntry.builder()
                .timestamp(Instant.now())
                .eventType(AuditEventType.RULE_DELETED)
                .ruleId(ruleId)
                .ruleName(ruleName)
                .build();

        logEntry(entry);
        logger.info("Audit: Rule deleted - {}", ruleName);
    }

    /**
     * Log error
     */
    public void logError(String ruleId, String ruleName, String error) {
        if (!enabled) {
            return;
        }

        AuditEntry entry = AuditEntry.builder()
                .timestamp(Instant.now())
                .eventType(AuditEventType.RULE_ERROR)
                .ruleId(ruleId)
                .ruleName(ruleName)
                .metadata(Map.of("error", error))
                .build();

        logEntry(entry);
        logger.error("Audit: Rule error - {} - {}", ruleName, error);
    }

    /**
     * Store audit entry
     */
    private void logEntry(AuditEntry entry) {
        auditLog.computeIfAbsent(entry.ruleId(), k -> new ArrayList<>()).add(entry);

        // Limit size per rule (keep last 1000 entries)
        List<AuditEntry> entries = auditLog.get(entry.ruleId());
        if (entries.size() > 1000) {
            entries.remove(0);
        }
    }

    /**
     * Get audit log for rule
     */
    public List<AuditEntry> getAuditLog(String ruleId) {
        return new ArrayList<>(auditLog.getOrDefault(ruleId, List.of()));
    }

    /**
     * Get audit log for rule (limited)
     */
    public List<AuditEntry> getAuditLog(String ruleId, int limit) {
        List<AuditEntry> entries = auditLog.getOrDefault(ruleId, List.of());
        int fromIndex = Math.max(0, entries.size() - limit);
        return new ArrayList<>(entries.subList(fromIndex, entries.size()));
    }

    /**
     * Get all audit entries
     */
    public List<AuditEntry> getAllAuditLogs() {
        return auditLog.values().stream()
                .flatMap(List::stream)
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .collect(Collectors.toList());
    }

    /**
     * Get audit statistics
     */
    public AuditStatistics getStatistics() {
        long totalEntries = auditLog.values().stream().mapToLong(List::size).sum();
        long totalExecutions = auditLog.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.eventType() == AuditEventType.RULE_EXECUTION)
                .count();
        long totalErrors = auditLog.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.eventType() == AuditEventType.RULE_ERROR)
                .count();

        return new AuditStatistics(
                totalEntries,
                totalExecutions,
                totalErrors,
                auditLog.size()
        );
    }

    /**
     * Get total executions
     */
    public long getTotalExecutions() {
        return auditLog.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.eventType() == AuditEventType.RULE_EXECUTION)
                .count();
    }

    /**
     * Get total matches
     */
    public long getTotalMatches() {
        return auditLog.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.eventType() == AuditEventType.RULE_EXECUTION && Boolean.TRUE.equals(e.matched()))
                .count();
    }

    /**
     * Get total errors
     */
    public long getTotalErrors() {
        return auditLog.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.eventType() == AuditEventType.RULE_ERROR)
                .count();
    }

    /**
     * Get average execution time
     */
    public double getAverageExecutionTime() {
        return auditLog.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.eventType() == AuditEventType.RULE_EXECUTION && e.executionTimeMs() != null)
                .mapToLong(AuditEntry::executionTimeMs)
                .average()
                .orElse(0.0);
    }

    /**
     * Get success rate (percentage of executions that matched)
     */
    public double getSuccessRate() {
        long total = getTotalExecutions();
        if (total == 0) {
            return 0.0;
        }
        long matches = getTotalMatches();
        return (matches * 100.0) / total;
    }

    /**
     * Get recent executions (limited)
     */
    public List<AuditEntry> getRecentExecutions(int limit) {
        return auditLog.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.eventType() == AuditEventType.RULE_EXECUTION)
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get executions by hour (last 24 hours)
     */
    public Map<String, Long> getExecutionsByHour() {
        Instant now = Instant.now();
        Instant dayAgo = now.minusSeconds(86400);

        return auditLog.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.eventType() == AuditEventType.RULE_EXECUTION)
                .filter(e -> e.timestamp().isAfter(dayAgo))
                .collect(Collectors.groupingBy(
                        e -> String.valueOf(e.timestamp().getEpochSecond() / 3600),
                        Collectors.counting()
                ));
    }

    /**
     * Get executions by status
     */
    public Map<String, Long> getExecutionsByStatus() {
        return auditLog.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.eventType() == AuditEventType.RULE_EXECUTION)
                .collect(Collectors.groupingBy(
                        e -> e.status() != null ? e.status() : "UNKNOWN",
                        Collectors.counting()
                ));
    }

    /**
     * Get execution statistics for a specific rule
     */
    public RuleExecutionStats getRuleExecutionStats(String ruleId) {
        List<AuditEntry> entries = auditLog.getOrDefault(ruleId, List.of()).stream()
                .filter(e -> e.eventType() == AuditEventType.RULE_EXECUTION)
                .toList();

        long executionCount = entries.size();
        long matchCount = entries.stream().filter(e -> Boolean.TRUE.equals(e.matched())).count();
        double avgTime = entries.stream()
                .filter(e -> e.executionTimeMs() != null)
                .mapToLong(AuditEntry::executionTimeMs)
                .average()
                .orElse(0.0);
        long errorCount = auditLog.getOrDefault(ruleId, List.of()).stream()
                .filter(e -> e.eventType() == AuditEventType.RULE_ERROR)
                .count();

        return new RuleExecutionStats(
                ruleId,
                executionCount,
                matchCount,
                avgTime,
                errorCount,
                executionCount > 0 ? (matchCount * 100.0) / executionCount : 0.0
        );
    }

    public record RuleExecutionStats(
            String ruleId,
            long executionCount,
            long matchCount,
            double avgExecutionTimeMs,
            long errorCount,
            double matchRate
    ) {}

    /**
     * Clear audit log
     */
    public void clear() {
        auditLog.clear();
    }

    /**
     * Clear audit log for specific rule
     */
    public void clearRule(String ruleId) {
        auditLog.remove(ruleId);
    }

    public record AuditEntry(
            Instant timestamp,
            AuditEventType eventType,
            String ruleId,
            String ruleName,
            String contextId,
            Boolean matched,
            Long executionTimeMs,
            String status,
            Map<String, Object> metadata
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Instant timestamp;
            private AuditEventType eventType;
            private String ruleId;
            private String ruleName;
            private String contextId;
            private Boolean matched;
            private Long executionTimeMs;
            private String status;
            private Map<String, Object> metadata = Map.of();

            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder eventType(AuditEventType eventType) {
                this.eventType = eventType;
                return this;
            }

            public Builder ruleId(String ruleId) {
                this.ruleId = ruleId;
                return this;
            }

            public Builder ruleName(String ruleName) {
                this.ruleName = ruleName;
                return this;
            }

            public Builder contextId(String contextId) {
                this.contextId = contextId;
                return this;
            }

            public Builder matched(Boolean matched) {
                this.matched = matched;
                return this;
            }

            public Builder executionTimeMs(Long executionTimeMs) {
                this.executionTimeMs = executionTimeMs;
                return this;
            }

            public Builder status(String status) {
                this.status = status;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }

            public AuditEntry build() {
                return new AuditEntry(
                        timestamp, eventType, ruleId, ruleName, contextId,
                        matched, executionTimeMs, status, metadata
                );
            }
        }
    }

    public enum AuditEventType {
        RULE_EXECUTION,
        RULE_CREATED,
        RULE_UPDATED,
        RULE_DELETED,
        RULE_ERROR,
        RULE_RELOADED
    }

    public record AuditStatistics(
            long totalEntries,
            long totalExecutions,
            long totalErrors,
            long trackedRules
    ) {}
}
