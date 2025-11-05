package com.ruleengine.scheduler;

import com.ruleengine.datasource.DataEnrichmentEngine;
import com.ruleengine.datasource.EnrichmentPlan;
import com.ruleengine.model.Rule;
import com.ruleengine.model.RuleContext;
import com.ruleengine.model.RuleResult;
import com.ruleengine.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduled Rule Executor
 *
 * Execute rules on a schedule:
 * - Cron-based scheduling
 * - Fixed rate execution
 * - Data enrichment before execution
 * - Result collection and notifications
 *
 * Examples:
 * - Run daily credit check at midnight
 * - Execute fraud detection every 5 minutes
 * - Generate reports weekly
 */
@Component
public class ScheduledRuleExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledRuleExecutor.class);

    private final RuleService ruleService;
    private final DataEnrichmentEngine enrichmentEngine;
    private final Map<String, ScheduledRuleJob> scheduledJobs;

    public ScheduledRuleExecutor(
            RuleService ruleService,
            DataEnrichmentEngine enrichmentEngine) {
        this.ruleService = ruleService;
        this.enrichmentEngine = enrichmentEngine;
        this.scheduledJobs = new ConcurrentHashMap<>();
    }

    /**
     * Schedule a rule to execute on cron schedule
     */
    public void scheduleRule(ScheduledRuleJob job) {
        scheduledJobs.put(job.getId(), job);
        logger.info("Scheduled rule job: {} with cron: {}", job.getName(), job.getCronExpression());
    }

    /**
     * Execute scheduled jobs (called by Spring scheduler)
     */
    @Scheduled(fixedRate = 60000) // Check every minute
    public void executeScheduledJobs() {
        LocalDateTime now = LocalDateTime.now();

        for (ScheduledRuleJob job : scheduledJobs.values()) {
            if (!job.isEnabled()) {
                continue;
            }

            if (shouldExecuteNow(job, now)) {
                executeJob(job);
            }
        }
    }

    /**
     * Check if job should execute now
     */
    private boolean shouldExecuteNow(ScheduledRuleJob job, LocalDateTime now) {
        try {
            CronExpression cron = CronExpression.parse(job.getCronExpression());
            LocalDateTime next = cron.next(job.getLastExecuted() != null
                    ? job.getLastExecuted()
                    : now.minusMinutes(1));

            return next != null && !next.isAfter(now);
        } catch (Exception e) {
            logger.error("Invalid cron expression: {}", job.getCronExpression(), e);
            return false;
        }
    }

    /**
     * Execute a scheduled job
     */
    private void executeJob(ScheduledRuleJob job) {
        logger.info("Executing scheduled job: {}", job.getName());

        try {
            // Create context
            RuleContext context = new RuleContext();

            // Enrich context if enrichment plan specified
            if (job.getEnrichmentPlan() != null) {
                enrichmentEngine.enrichContext(context, job.getEnrichmentPlan());
            }

            // Execute rules
            List<RuleResult> results;
            if (job.getRuleGroup() != null) {
                results = ruleService.executeRuleGroup(job.getRuleGroup(), context);
            } else if (job.getRuleId() != null) {
                results = List.of(ruleService.executeRule(job.getRuleId(), context));
            } else {
                logger.warn("Job has no rule or rule group: {}", job.getName());
                return;
            }

            // Process results
            job.setLastExecuted(LocalDateTime.now());
            job.setLastResults(results);
            job.incrementExecutionCount();

            // Notify if callback specified
            if (job.getCallback() != null) {
                job.getCallback().onComplete(results);
            }

            logger.info("Job completed: {} - {} rules executed, {} matched",
                    job.getName(),
                    results.size(),
                    results.stream().filter(RuleResult::isMatched).count());

        } catch (Exception e) {
            logger.error("Job execution failed: {}", job.getName(), e);
            job.setLastError(e.getMessage());
        }
    }

    /**
     * Remove scheduled job
     */
    public void removeScheduledJob(String jobId) {
        scheduledJobs.remove(jobId);
        logger.info("Removed scheduled job: {}", jobId);
    }

    /**
     * Get all scheduled jobs
     */
    public Collection<ScheduledRuleJob> getScheduledJobs() {
        return new ArrayList<>(scheduledJobs.values());
    }

    /**
     * Get job statistics
     */
    public JobStatistics getStatistics() {
        long totalJobs = scheduledJobs.size();
        long enabledJobs = scheduledJobs.values().stream()
                .filter(ScheduledRuleJob::isEnabled)
                .count();
        long totalExecutions = scheduledJobs.values().stream()
                .mapToLong(ScheduledRuleJob::getExecutionCount)
                .sum();

        return new JobStatistics(totalJobs, enabledJobs, totalExecutions);
    }

    public record JobStatistics(long totalJobs, long enabledJobs, long totalExecutions) {}
}
