package com.ruleengine.service;

import com.ruleengine.cache.RuleCache;
import com.ruleengine.core.RuleEngine;
import com.ruleengine.model.Rule;
import com.ruleengine.model.RuleContext;
import com.ruleengine.model.RuleResult;
import com.ruleengine.parser.JsonRuleParser;
import com.ruleengine.repository.RuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Rule Service
 *
 * High-level service for rule management and execution
 */
@Service
public class RuleService {

    private static final Logger logger = LoggerFactory.getLogger(RuleService.class);

    private final RuleRepository ruleRepository;
    private final RuleEngine ruleEngine;
    private final RuleCache ruleCache;
    private final JsonRuleParser ruleParser;

    public RuleService(
            RuleRepository ruleRepository,
            RuleEngine ruleEngine,
            RuleCache ruleCache,
            JsonRuleParser ruleParser) {
        this.ruleRepository = ruleRepository;
        this.ruleEngine = ruleEngine;
        this.ruleCache = ruleCache;
        this.ruleParser = ruleParser;
    }

    /**
     * Create a new rule
     */
    public Rule createRule(Rule rule) {
        logger.info("Creating rule: {}", rule.getName());
        Rule saved = ruleRepository.save(rule);
        ruleCache.putRule(saved.getId(), saved);
        return saved;
    }

    /**
     * Create rule from JSON
     */
    public Rule createRuleFromJson(String json) {
        Rule rule = ruleParser.parse(json);
        return createRule(rule);
    }

    /**
     * Update a rule
     */
    public Rule updateRule(String id, Rule rule) {
        Optional<Rule> existing = ruleRepository.findById(id);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Rule not found: " + id);
        }

        rule.setId(id);
        Rule updated = ruleRepository.update(rule);

        // Invalidate cache
        ruleCache.invalidateRule(id);
        if (rule.getRuleGroup() != null) {
            ruleCache.invalidateRuleGroup(rule.getRuleGroup());
        }

        logger.info("Updated rule: {}", id);
        return updated;
    }

    /**
     * Delete a rule
     */
    public void deleteRule(String id) {
        Optional<Rule> rule = ruleRepository.findById(id);
        rule.ifPresent(r -> {
            ruleRepository.deleteById(id);
            ruleCache.invalidateRule(id);
            if (r.getRuleGroup() != null) {
                ruleCache.invalidateRuleGroup(r.getRuleGroup());
            }
            logger.info("Deleted rule: {}", id);
        });
    }

    /**
     * Get rule by ID
     */
    public Optional<Rule> getRule(String id) {
        // Try cache first
        Optional<Rule> cached = ruleCache.getRule(id);
        if (cached.isPresent()) {
            return cached;
        }

        // Fall back to repository
        Optional<Rule> rule = ruleRepository.findById(id);
        rule.ifPresent(r -> ruleCache.putRule(id, r));
        return rule;
    }

    /**
     * Get all rules
     */
    public List<Rule> getAllRules() {
        return ruleRepository.findAll();
    }

    /**
     * Get rules by group
     */
    public List<Rule> getRulesByGroup(String group) {
        // Try cache first
        Optional<List<Rule>> cached = ruleCache.getRuleGroup(group);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Fall back to repository
        List<Rule> rules = ruleRepository.findByGroup(group);
        ruleCache.putRuleGroup(group, rules);
        return rules;
    }

    /**
     * Get rules by tenant
     */
    public List<Rule> getRulesByTenant(String tenant) {
        return ruleRepository.findByTenant(tenant);
    }

    /**
     * Execute a single rule
     */
    public RuleResult executeRule(String ruleId, RuleContext context) {
        Optional<Rule> rule = getRule(ruleId);
        if (rule.isEmpty()) {
            throw new IllegalArgumentException("Rule not found: " + ruleId);
        }

        return ruleEngine.execute(rule.get(), context);
    }

    /**
     * Execute all rules in a group
     */
    public List<RuleResult> executeRuleGroup(String group, RuleContext context) {
        List<Rule> rules = getRulesByGroup(group);
        return ruleEngine.executePrioritized(rules, context);
    }

    /**
     * Execute all enabled rules for a tenant
     */
    public List<RuleResult> executeForTenant(String tenant, RuleContext context) {
        List<Rule> rules = ruleRepository.findByTenant(tenant).stream()
                .filter(Rule::getEnabled)
                .toList();
        return ruleEngine.executePrioritized(rules, context);
    }

    /**
     * Test a rule without saving
     */
    public RuleResult testRule(Rule rule, RuleContext context) {
        return ruleEngine.execute(rule, context);
    }

    /**
     * Test rule from JSON
     */
    public RuleResult testRuleFromJson(String json, RuleContext context) {
        Rule rule = ruleParser.parse(json);
        return testRule(rule, context);
    }

    /**
     * Import rules from JSON array
     */
    public List<Rule> importRules(String json) {
        List<Rule> rules = ruleParser.parseMultiple(json);
        rules.forEach(this::createRule);
        return rules;
    }

    /**
     * Export rule to JSON
     */
    public String exportRule(String id) {
        Optional<Rule> rule = getRule(id);
        if (rule.isEmpty()) {
            throw new IllegalArgumentException("Rule not found: " + id);
        }
        return ruleParser.toJson(rule.get());
    }

    /**
     * Get engine statistics
     */
    public RuleEngine.EngineStatistics getStatistics() {
        return ruleEngine.getStatistics();
    }

    /**
     * Get cache statistics
     */
    public RuleCache.CacheStatistics getCacheStatistics() {
        return ruleCache.getStatistics();
    }

    /**
     * Clear all caches
     */
    public void clearCache() {
        ruleCache.clearAll();
        logger.info("Cleared all caches");
    }
}
