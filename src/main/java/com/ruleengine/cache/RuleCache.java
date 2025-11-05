package com.ruleengine.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ruleengine.model.Rule;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Rule Cache
 *
 * High-performance caching for hot rules
 * Uses Caffeine for in-memory caching
 */
@Component
public class RuleCache {

    private final Cache<String, Rule> ruleCache;
    private final Cache<String, List<Rule>> ruleGroupCache;
    private final Cache<String, Object> compiledExpressionCache;

    public RuleCache() {
        // Rule cache - keep hot rules in memory
        this.ruleCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build();

        // Rule group cache
        this.ruleGroupCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats()
                .build();

        // Compiled expression cache
        this.compiledExpressionCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(Duration.ofHours(2))
                .recordStats()
                .build();
    }

    /**
     * Get rule from cache
     */
    public Optional<Rule> getRule(String ruleId) {
        return Optional.ofNullable(ruleCache.getIfPresent(ruleId));
    }

    /**
     * Put rule in cache
     */
    public void putRule(String ruleId, Rule rule) {
        ruleCache.put(ruleId, rule);
    }

    /**
     * Invalidate rule
     */
    public void invalidateRule(String ruleId) {
        ruleCache.invalidate(ruleId);
    }

    /**
     * Get rule group from cache
     */
    public Optional<List<Rule>> getRuleGroup(String groupKey) {
        return Optional.ofNullable(ruleGroupCache.getIfPresent(groupKey));
    }

    /**
     * Put rule group in cache
     */
    public void putRuleGroup(String groupKey, List<Rule> rules) {
        ruleGroupCache.put(groupKey, rules);
    }

    /**
     * Invalidate rule group
     */
    public void invalidateRuleGroup(String groupKey) {
        ruleGroupCache.invalidate(groupKey);
    }

    /**
     * Get compiled expression from cache
     */
    public Optional<Object> getCompiledExpression(String expression) {
        return Optional.ofNullable(compiledExpressionCache.getIfPresent(expression));
    }

    /**
     * Put compiled expression in cache
     */
    public void putCompiledExpression(String expression, Object compiled) {
        compiledExpressionCache.put(expression, compiled);
    }

    /**
     * Clear all caches
     */
    public void clearAll() {
        ruleCache.invalidateAll();
        ruleGroupCache.invalidateAll();
        compiledExpressionCache.invalidateAll();
    }

    /**
     * Get cache statistics
     */
    public CacheStatistics getStatistics() {
        var ruleStats = ruleCache.stats();
        var groupStats = ruleGroupCache.stats();
        var exprStats = compiledExpressionCache.stats();

        return new CacheStatistics(
                ruleStats.hitCount() + groupStats.hitCount() + exprStats.hitCount(),
                ruleStats.missCount() + groupStats.missCount() + exprStats.missCount(),
                ruleStats.hitRate(),
                ruleCache.estimatedSize() + ruleGroupCache.estimatedSize() + compiledExpressionCache.estimatedSize()
        );
    }

    public record CacheStatistics(
            long hits,
            long misses,
            double hitRate,
            long size
    ) {}
}
