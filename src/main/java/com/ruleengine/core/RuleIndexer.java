package com.ruleengine.core;

import com.ruleengine.model.Condition;
import com.ruleengine.model.Rule;
import com.ruleengine.model.RuleContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rule Indexer - RETE-like algorithm for fast rule matching
 *
 * Builds an index tree for O(1) rule lookups based on fact patterns
 * Instead of evaluating all rules, find matching rules instantly
 */
@Component
public class RuleIndexer {

    // Index: field -> operator -> value -> List<Rule>
    private final Map<String, Map<Condition.Operator, Map<Object, Set<String>>>> fieldIndex;

    // Index: ruleId -> Rule
    private final Map<String, Rule> ruleMap;

    // Index for expression-based rules (cannot be indexed by field)
    private final Set<String> expressionRules;

    public RuleIndexer() {
        this.fieldIndex = new ConcurrentHashMap<>();
        this.ruleMap = new ConcurrentHashMap<>();
        this.expressionRules = ConcurrentHashMap.newKeySet();
    }

    /**
     * Index a rule for fast lookup
     */
    public void indexRule(Rule rule) {
        ruleMap.put(rule.getId(), rule);

        for (Condition condition : rule.getConditions()) {
            if (condition.isSimple()) {
                indexSimpleCondition(rule.getId(), condition);
            } else if (condition.isExpression()) {
                expressionRules.add(rule.getId());
            } else if (condition.isNested()) {
                // Index nested conditions recursively
                indexNestedConditions(rule.getId(), condition.getNestedConditions());
            }
        }
    }

    private void indexSimpleCondition(String ruleId, Condition condition) {
        String field = condition.getField();
        Condition.Operator operator = condition.getOperator();
        Object value = condition.getValue();

        fieldIndex
                .computeIfAbsent(field, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(operator, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(value, k -> ConcurrentHashMap.newKeySet())
                .add(ruleId);
    }

    private void indexNestedConditions(String ruleId, List<Condition> conditions) {
        for (Condition condition : conditions) {
            if (condition.isSimple()) {
                indexSimpleCondition(ruleId, condition);
            }
        }
    }

    /**
     * Remove rule from index
     */
    public void removeRule(String ruleId) {
        ruleMap.remove(ruleId);
        expressionRules.remove(ruleId);

        // Remove from field index
        fieldIndex.values().forEach(operatorMap ->
                operatorMap.values().forEach(valueMap ->
                        valueMap.values().forEach(ruleSet -> ruleSet.remove(ruleId))
                )
        );
    }

    /**
     * Find candidate rules based on context (fast lookup)
     */
    public List<Rule> findCandidateRules(RuleContext context) {
        Set<String> candidateIds = new HashSet<>();

        // Add all expression-based rules (cannot be indexed)
        candidateIds.addAll(expressionRules);

        // Find rules matching facts in context
        context.getFacts().forEach((factKey, fact) -> {
            fact.getData().forEach((field, value) -> {
                String fullField = factKey + "." + field;

                // Look up rules indexed by this field
                Map<Condition.Operator, Map<Object, Set<String>>> operatorMap = fieldIndex.get(fullField);
                if (operatorMap != null) {
                    // For each operator, check if value matches
                    operatorMap.forEach((operator, valueMap) -> {
                        Set<String> rules = valueMap.get(value);
                        if (rules != null) {
                            candidateIds.addAll(rules);
                        }
                    });
                }
            });
        });

        // Convert IDs to Rule objects
        return candidateIds.stream()
                .map(ruleMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Get all indexed rules
     */
    public List<Rule> getAllRules() {
        return new ArrayList<>(ruleMap.values());
    }

    /**
     * Get index statistics
     */
    public IndexStatistics getStatistics() {
        int totalIndexedFields = fieldIndex.size();
        int totalIndexedRules = ruleMap.size();
        int expressionBasedRules = expressionRules.size();

        return new IndexStatistics(
                totalIndexedRules,
                totalIndexedFields,
                expressionBasedRules,
                totalIndexedRules - expressionBasedRules
        );
    }

    /**
     * Clear all indexes
     */
    public void clear() {
        fieldIndex.clear();
        ruleMap.clear();
        expressionRules.clear();
    }

    /**
     * Rebuild index from rules
     */
    public void rebuildIndex(List<Rule> rules) {
        clear();
        rules.forEach(this::indexRule);
    }

    public record IndexStatistics(
            int totalRules,
            int indexedFields,
            int expressionRules,
            int indexableRules
    ) {}
}
