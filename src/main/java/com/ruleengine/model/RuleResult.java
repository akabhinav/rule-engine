package com.ruleengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule Result - Result of rule evaluation
 *
 * Contains:
 * - Match status
 * - Execution metadata
 * - Action results
 * - Enrichment data
 * - Performance metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {

    private String ruleId;
    private String ruleName;
    private String contextId;

    // Execution status
    private boolean matched;
    private ExecutionStatus status;

    // Timing
    private Instant executedAt;
    private Long executionTimeMs;

    // Condition evaluation details
    @Builder.Default
    private List<ConditionResult> conditionResults = new ArrayList<>();

    // Action execution results
    @Builder.Default
    private List<ActionResult> actionResults = new ArrayList<>();

    // Enrichment data (additional data to return)
    @Builder.Default
    private Map<String, Object> enrichmentData = new HashMap<>();

    // Error details
    private String errorMessage;
    private String stackTrace;

    // Metadata
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public enum ExecutionStatus {
        SUCCESS,
        MATCHED,
        NOT_MATCHED,
        ERROR,
        SKIPPED,
        TIMEOUT
    }

    /**
     * Add condition result
     */
    public void addConditionResult(String conditionId, boolean matched, Object actualValue) {
        conditionResults.add(ConditionResult.builder()
                .conditionId(conditionId)
                .matched(matched)
                .actualValue(actualValue)
                .build());
    }

    /**
     * Add action result
     */
    public void addActionResult(String actionId, boolean success, Object result) {
        actionResults.add(ActionResult.builder()
                .actionId(actionId)
                .success(success)
                .result(result)
                .build());
    }

    /**
     * Add enrichment data
     */
    public void addEnrichment(String key, Object value) {
        enrichmentData.put(key, value);
    }

    /**
     * Set error
     */
    public void setError(String message, Exception e) {
        this.status = ExecutionStatus.ERROR;
        this.errorMessage = message;
        this.stackTrace = e != null ? getStackTraceAsString(e) : null;
    }

    private String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Create success result
     */
    public static RuleResult success(String ruleId, String ruleName, boolean matched) {
        return RuleResult.builder()
                .ruleId(ruleId)
                .ruleName(ruleName)
                .matched(matched)
                .status(matched ? ExecutionStatus.MATCHED : ExecutionStatus.NOT_MATCHED)
                .executedAt(Instant.now())
                .build();
    }

    /**
     * Create error result
     */
    public static RuleResult error(String ruleId, String ruleName, String errorMessage) {
        return RuleResult.builder()
                .ruleId(ruleId)
                .ruleName(ruleName)
                .matched(false)
                .status(ExecutionStatus.ERROR)
                .errorMessage(errorMessage)
                .executedAt(Instant.now())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionResult {
        private String conditionId;
        private boolean matched;
        private Object expectedValue;
        private Object actualValue;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionResult {
        private String actionId;
        private boolean success;
        private Object result;
        private String errorMessage;
    }
}
