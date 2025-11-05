package com.ruleengine.datasource;

import com.ruleengine.model.Fact;
import com.ruleengine.model.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * Data Enrichment Engine
 *
 * Enriches rule context with data from multiple sources:
 * - Fetches data from databases
 * - Calls REST APIs
 * - Reads files
 * - Caches results
 * - Parallel data fetching
 *
 * Example:
 * - Fetch user profile from database
 * - Get credit score from external API
 * - Load historical transactions
 * - Combine all data for rule evaluation
 */
@Component
public class DataEnrichmentEngine {

    private static final Logger logger = LoggerFactory.getLogger(DataEnrichmentEngine.class);

    private final DataSourceRegistry registry;
    private final ExecutorService executorService;
    private final Map<String, Object> cache;

    public DataEnrichmentEngine(DataSourceRegistry registry) {
        this.registry = registry;
        this.executorService = Executors.newFixedThreadPool(10);
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Enrich context with data from multiple sources
     */
    public void enrichContext(RuleContext context, EnrichmentPlan plan) {
        logger.debug("Enriching context with {} sources", plan.getSteps().size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (EnrichmentStep step : plan.getSteps()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    executeEnrichmentStep(context, step);
                } catch (Exception e) {
                    logger.error("Enrichment step failed: {}", step.getName(), e);
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all enrichments to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        logger.debug("Context enrichment completed");
    }

    /**
     * Execute a single enrichment step
     */
    private void executeEnrichmentStep(RuleContext context, EnrichmentStep step) {
        // Check cache first
        String cacheKey = buildCacheKey(step);
        if (step.isCacheEnabled() && cache.containsKey(cacheKey)) {
            logger.debug("Cache hit for: {}", step.getName());
            addDataToContext(context, step.getFactKey(), cache.get(cacheKey));
            return;
        }

        // Fetch data from source
        DataSource dataSource = registry.getDataSource(step.getDataSourceName());
        if (dataSource == null) {
            logger.warn("Data source not found: {}", step.getDataSourceName());
            return;
        }

        List<Map<String, Object>> data = dataSource.query(step.getQuery());

        // Transform data if needed
        Object enrichedData = step.getTransform() != null
                ? step.getTransform().apply(data)
                : (data.size() == 1 ? data.get(0) : data);

        // Add to context
        addDataToContext(context, step.getFactKey(), enrichedData);

        // Cache the result
        if (step.isCacheEnabled()) {
            cache.put(cacheKey, enrichedData);
        }

        logger.debug("Enrichment step completed: {}", step.getName());
    }

    /**
     * Add enriched data to context as a Fact
     */
    private void addDataToContext(RuleContext context, String factKey, Object data) {
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapData = (Map<String, Object>) data;
            context.addFact(factKey, new Fact(factKey, mapData));
        } else if (data instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> listData = (List<Map<String, Object>>) data;
            context.addFact(factKey, new Fact(factKey, Map.of("data", listData)));
        } else {
            context.addFact(factKey, new Fact(factKey, Map.of("value", data)));
        }
    }

    /**
     * Build cache key
     */
    private String buildCacheKey(EnrichmentStep step) {
        return step.getDataSourceName() + ":" + step.getQuery().hashCode();
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        cache.clear();
        logger.info("Enrichment cache cleared");
    }

    /**
     * Shutdown executor
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
