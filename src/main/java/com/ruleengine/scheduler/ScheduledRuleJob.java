package com.ruleengine.scheduler;

import com.ruleengine.datasource.EnrichmentPlan;
import com.ruleengine.model.RuleResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled Rule Job
 *
 * Represents a scheduled rule execution task
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledRuleJob {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String name;
    private String description;

    // Schedule
    private String cronExpression; // e.g., "0 0 * * * *" = hourly

    // Rule to execute
    private String ruleId;
    private String ruleGroup;

    // Data enrichment
    private EnrichmentPlan enrichmentPlan;

    // Status
    @Builder.Default
    private boolean enabled = true;

    private LocalDateTime lastExecuted;
    private List<RuleResult> lastResults;
    private String lastError;

    @Builder.Default
    private long executionCount = 0;

    // Callback for results
    private JobCallback callback;

    public void incrementExecutionCount() {
        executionCount++;
    }

    /**
     * Job callback interface
     */
    @FunctionalInterface
    public interface JobCallback {
        void onComplete(List<RuleResult> results);
    }

    /**
     * Create hourly job
     */
    public static ScheduledRuleJob hourly(String name, String ruleGroup) {
        return ScheduledRuleJob.builder()
                .name(name)
                .cronExpression("0 0 * * * *")
                .ruleGroup(ruleGroup)
                .build();
    }

    /**
     * Create daily job
     */
    public static ScheduledRuleJob daily(String name, String ruleGroup, int hour) {
        return ScheduledRuleJob.builder()
                .name(name)
                .cronExpression(String.format("0 0 %d * * *", hour))
                .ruleGroup(ruleGroup)
                .build();
    }

    /**
     * Create weekly job
     */
    public static ScheduledRuleJob weekly(String name, String ruleGroup, int dayOfWeek, int hour) {
        return ScheduledRuleJob.builder()
                .name(name)
                .cronExpression(String.format("0 0 %d * * %d", hour, dayOfWeek))
                .ruleGroup(ruleGroup)
                .build();
    }
}
