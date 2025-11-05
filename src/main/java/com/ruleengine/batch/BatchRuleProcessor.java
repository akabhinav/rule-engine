package com.ruleengine.batch;

import com.ruleengine.datasource.DataQuery;
import com.ruleengine.datasource.DataSource;
import com.ruleengine.datasource.DataSourceRegistry;
import com.ruleengine.model.*;
import com.ruleengine.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Batch Rule Processor
 *
 * Process rules against large datasets in batches:
 * - Database batch processing
 * - Parallel processing
 * - Progress tracking
 * - Result aggregation
 * - Error handling
 *
 * Examples:
 * - Process 1 million transactions for fraud detection
 * - Score all customers for credit risk
 * - Validate entire product catalog
 */
@Component
public class BatchRuleProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BatchRuleProcessor.class);

    private final RuleService ruleService;
    private final DataSourceRegistry dataSourceRegistry;
    private final ExecutorService executorService;

    public BatchRuleProcessor(
            RuleService ruleService,
            DataSourceRegistry dataSourceRegistry) {
        this.ruleService = ruleService;
        this.dataSourceRegistry = dataSourceRegistry;
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

    /**
     * Process batch job
     */
    public BatchProcessResult processBatch(BatchProcessJob job) {
        logger.info("Starting batch process: {}", job.getName());

        Instant startTime = Instant.now();
        BatchProcessResult result = new BatchProcessResult(job.getName());

        try {
            // Fetch data from source
            DataSource dataSource = dataSourceRegistry.getDataSource(job.getDataSourceName());
            if (dataSource == null) {
                throw new IllegalArgumentException("Data source not found: " + job.getDataSourceName());
            }

            List<Map<String, Object>> data = dataSource.query(job.getQuery());
            logger.info("Fetched {} records for batch processing", data.size());

            // Process in batches
            int batchSize = job.getBatchSize();
            List<List<Map<String, Object>>> batches = partitionData(data, batchSize);

            List<CompletableFuture<BatchResult>> futures = new ArrayList<>();

            for (int i = 0; i < batches.size(); i++) {
                final int batchIndex = i;
                final List<Map<String, Object>> batch = batches.get(i);

                CompletableFuture<BatchResult> future = CompletableFuture.supplyAsync(() ->
                        processBatchChunk(job, batch, batchIndex), executorService);

                futures.add(future);
            }

            // Wait for all batches to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Aggregate results
            for (CompletableFuture<BatchResult> future : futures) {
                BatchResult batchResult = future.get();
                result.addBatchResult(batchResult);
            }

            result.setCompleted(true);
            result.setDurationMs(System.currentTimeMillis() - startTime.toEpochMilli());

            logger.info("Batch process completed: {} records processed, {} matched, {} errors",
                    result.getTotalProcessed(),
                    result.getTotalMatched(),
                    result.getTotalErrors());

        } catch (Exception e) {
            logger.error("Batch process failed: {}", job.getName(), e);
            result.setCompleted(false);
            result.setError(e.getMessage());
        }

        return result;
    }

    /**
     * Process a single batch chunk
     */
    private BatchResult processBatchChunk(
            BatchProcessJob job,
            List<Map<String, Object>> batch,
            int batchIndex) {

        BatchResult result = new BatchResult(batchIndex);
        int processed = 0;
        int matched = 0;
        int errors = 0;

        for (Map<String, Object> record : batch) {
            try {
                // Create context from record
                RuleContext context = RuleContext.withFact(job.getFactKey(), record);

                // Execute rules
                List<RuleResult> ruleResults;
                if (job.getRuleGroup() != null) {
                    ruleResults = ruleService.executeRuleGroup(job.getRuleGroup(), context);
                } else if (job.getRuleId() != null) {
                    ruleResults = List.of(ruleService.executeRule(job.getRuleId(), context));
                } else {
                    logger.warn("No rule or rule group specified");
                    errors++;
                    continue;
                }

                // Count matches
                long matchCount = ruleResults.stream()
                        .filter(RuleResult::isMatched)
                        .count();

                if (matchCount > 0) {
                    matched++;
                    result.addMatchedResult(record, ruleResults);
                }

                processed++;

            } catch (Exception e) {
                logger.error("Error processing record in batch {}", batchIndex, e);
                errors++;
            }
        }

        result.setProcessed(processed);
        result.setMatched(matched);
        result.setErrors(errors);

        return result;
    }

    /**
     * Partition data into batches
     */
    private <T> List<List<T>> partitionData(List<T> data, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < data.size(); i += batchSize) {
            batches.add(data.subList(i, Math.min(i + batchSize, data.size())));
        }
        return batches;
    }

    /**
     * Shutdown executor
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Batch Result
     */
    public static class BatchResult {
        private final int batchIndex;
        private int processed;
        private int matched;
        private int errors;
        private final List<MatchedRecord> matchedRecords = new ArrayList<>();

        public BatchResult(int batchIndex) {
            this.batchIndex = batchIndex;
        }

        public void setProcessed(int processed) {
            this.processed = processed;
        }

        public void setMatched(int matched) {
            this.matched = matched;
        }

        public void setErrors(int errors) {
            this.errors = errors;
        }

        public void addMatchedResult(Map<String, Object> record, List<RuleResult> results) {
            matchedRecords.add(new MatchedRecord(record, results));
        }

        public int getProcessed() {
            return processed;
        }

        public int getMatched() {
            return matched;
        }

        public int getErrors() {
            return errors;
        }

        public List<MatchedRecord> getMatchedRecords() {
            return matchedRecords;
        }
    }

    /**
     * Matched Record
     */
    public record MatchedRecord(Map<String, Object> record, List<RuleResult> results) {}
}
