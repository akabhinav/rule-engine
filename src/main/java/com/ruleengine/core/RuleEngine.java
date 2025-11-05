package com.ruleengine.core;

import com.ruleengine.model.Rule;
import com.ruleengine.model.RuleContext;
import com.ruleengine.model.RuleResult;

import java.util.List;

/**
 * Rule Engine Interface
 *
 * Core contract for rule execution
 */
public interface RuleEngine {

    /**
     * Execute a single rule against context
     */
    RuleResult execute(Rule rule, RuleContext context);

    /**
     * Execute multiple rules against context
     * Returns all matching rule results
     */
    List<RuleResult> executeAll(List<Rule> rules, RuleContext context);

    /**
     * Execute rules with early termination on first match
     */
    RuleResult executeFirst(List<Rule> rules, RuleContext context);

    /**
     * Execute rules in priority order
     */
    List<RuleResult> executePrioritized(List<Rule> rules, RuleContext context);

    /**
     * Execute rules with dependency resolution (DAG)
     */
    List<RuleResult> executeWithDependencies(List<Rule> rules, RuleContext context);

    /**
     * Get engine statistics
     */
    EngineStatistics getStatistics();

    /**
     * Engine Statistics
     */
    record EngineStatistics(
        long totalExecutions,
        long totalMatches,
        long totalErrors,
        double avgExecutionTimeMs,
        long cacheHits,
        long cacheMisses
    ) {}
}
