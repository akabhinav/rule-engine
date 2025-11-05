package com.ruleengine.batch;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch Process Result
 *
 * Aggregated results from batch processing
 */
@Data
public class BatchProcessResult {

    private final String jobName;
    private final Instant startTime;

    private boolean completed;
    private long durationMs;
    private String error;

    private final List<BatchRuleProcessor.BatchResult> batchResults = new ArrayList<>();

    public BatchProcessResult(String jobName) {
        this.jobName = jobName;
        this.startTime = Instant.now();
    }

    /**
     * Add batch result
     */
    public void addBatchResult(BatchRuleProcessor.BatchResult result) {
        batchResults.add(result);
    }

    /**
     * Get total records processed
     */
    public int getTotalProcessed() {
        return batchResults.stream()
                .mapToInt(BatchRuleProcessor.BatchResult::getProcessed)
                .sum();
    }

    /**
     * Get total matched records
     */
    public int getTotalMatched() {
        return batchResults.stream()
                .mapToInt(BatchRuleProcessor.BatchResult::getMatched)
                .sum();
    }

    /**
     * Get total errors
     */
    public int getTotalErrors() {
        return batchResults.stream()
                .mapToInt(BatchRuleProcessor.BatchResult::getErrors)
                .sum();
    }

    /**
     * Get all matched records
     */
    public List<BatchRuleProcessor.MatchedRecord> getAllMatchedRecords() {
        return batchResults.stream()
                .flatMap(br -> br.getMatchedRecords().stream())
                .toList();
    }

    /**
     * Get success rate
     */
    public double getSuccessRate() {
        int total = getTotalProcessed();
        return total > 0 ? (double) (total - getTotalErrors()) / total : 0.0;
    }

    /**
     * Get match rate
     */
    public double getMatchRate() {
        int total = getTotalProcessed();
        return total > 0 ? (double) getTotalMatched() / total : 0.0;
    }
}
