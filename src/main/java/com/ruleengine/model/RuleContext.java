package com.ruleengine.model;

import lombok.Data;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rule Context - Execution context for rule evaluation
 *
 * Contains:
 * - Facts (input data)
 * - Context attributes (user, channel, region, etc.)
 * - Execution metadata
 * - Result accumulator
 */
@Data
public class RuleContext {

    private String contextId;
    private Instant createdAt;

    // Facts (schema-less data)
    private Map<String, Fact> facts;

    // Context attributes (for context matching)
    private Map<String, Object> contextAttributes;

    // Execution metadata
    private String tenant;
    private String channel;
    private String userId;
    private String sessionId;

    // Results accumulator (for rule chaining)
    private Map<String, Object> intermediateResults;

    // Execution tracking
    private List<String> executedRules;
    private Map<String, RuleResult> ruleResults;

    // Temporal data (for time-window rules)
    private List<Fact> historicalFacts;

    public RuleContext() {
        this.contextId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.facts = new ConcurrentHashMap<>();
        this.contextAttributes = new ConcurrentHashMap<>();
        this.intermediateResults = new ConcurrentHashMap<>();
        this.executedRules = Collections.synchronizedList(new ArrayList<>());
        this.ruleResults = new ConcurrentHashMap<>();
        this.historicalFacts = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Add a fact to the context
     */
    public void addFact(String key, Fact fact) {
        facts.put(key, fact);
    }

    /**
     * Get fact by key
     */
    public Fact getFact(String key) {
        return facts.get(key);
    }

    /**
     * Get value from fact by path
     * Format: "factKey.field.subfield"
     */
    public Object getFactValue(String path) {
        if (path == null || !path.contains(".")) {
            return null;
        }

        String[] parts = path.split("\\.", 2);
        String factKey = parts[0];
        String fieldPath = parts.length > 1 ? parts[1] : "";

        Fact fact = facts.get(factKey);
        if (fact == null) {
            return null;
        }

        return fieldPath.isEmpty() ? fact.getData() : fact.getValue(fieldPath);
    }

    /**
     * Set context attribute
     */
    public void setContextAttribute(String key, Object value) {
        contextAttributes.put(key, value);
    }

    /**
     * Get context attribute
     */
    public Object getContextAttribute(String key) {
        return contextAttributes.get(key);
    }

    /**
     * Set intermediate result (for rule chaining)
     */
    public void setIntermediateResult(String key, Object value) {
        intermediateResults.put(key, value);
    }

    /**
     * Get intermediate result
     */
    public Object getIntermediateResult(String key) {
        return intermediateResults.get(key);
    }

    /**
     * Mark rule as executed
     */
    public void markRuleExecuted(String ruleId) {
        executedRules.add(ruleId);
    }

    /**
     * Check if rule was already executed
     */
    public boolean wasRuleExecuted(String ruleId) {
        return executedRules.contains(ruleId);
    }

    /**
     * Add rule result
     */
    public void addRuleResult(String ruleId, RuleResult result) {
        ruleResults.put(ruleId, result);
    }

    /**
     * Add historical fact (for temporal rules)
     */
    public void addHistoricalFact(Fact fact) {
        historicalFacts.add(fact);
    }

    /**
     * Get historical facts within time window
     */
    public List<Fact> getHistoricalFacts(Instant from, Instant to) {
        return historicalFacts.stream()
                .filter(f -> f.getTimestamp() != null)
                .filter(f -> !f.getTimestamp().isBefore(from) && !f.getTimestamp().isAfter(to))
                .toList();
    }

    /**
     * Create a simple context with one fact
     */
    public static RuleContext withFact(String key, Map<String, Object> data) {
        RuleContext context = new RuleContext();
        context.addFact(key, new Fact(key, data));
        return context;
    }

    /**
     * Create context with multiple facts
     */
    public static RuleContext withFacts(Map<String, Map<String, Object>> factsData) {
        RuleContext context = new RuleContext();
        factsData.forEach((key, data) -> context.addFact(key, new Fact(key, data)));
        return context;
    }
}
