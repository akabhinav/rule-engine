package com.ruleengine.repository;

import com.ruleengine.model.Rule;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-Memory Rule Repository
 *
 * Fast, memory-based rule storage
 * Perfect for hot-reload and high-performance scenarios
 */
@Repository
public class InMemoryRuleRepository implements RuleRepository {

    private final Map<String, Rule> rules = new ConcurrentHashMap<>();

    @Override
    public Rule save(Rule rule) {
        if (rule.getId() == null) {
            rule.setId(UUID.randomUUID().toString());
        }

        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(Instant.now());
        }

        rule.setUpdatedAt(Instant.now());

        rules.put(rule.getId(), rule);
        return rule;
    }

    @Override
    public Optional<Rule> findById(String id) {
        return Optional.ofNullable(rules.get(id));
    }

    @Override
    public List<Rule> findAll() {
        return new ArrayList<>(rules.values());
    }

    @Override
    public List<Rule> findByGroup(String group) {
        return rules.values().stream()
                .filter(r -> group.equals(r.getRuleGroup()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Rule> findByTenant(String tenant) {
        return rules.values().stream()
                .filter(r -> tenant.equals(r.getTenant()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Rule> findEnabled() {
        return rules.values().stream()
                .filter(Rule::getEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Rule> findByNameAndVersion(String name, Integer version) {
        return rules.values().stream()
                .filter(r -> name.equals(r.getName()) && version.equals(r.getVersion()))
                .findFirst();
    }

    @Override
    public Optional<Rule> findLatestVersion(String name) {
        return rules.values().stream()
                .filter(r -> name.equals(r.getName()))
                .max(Comparator.comparing(r -> r.getVersion() != null ? r.getVersion() : 0));
    }

    @Override
    public void deleteById(String id) {
        rules.remove(id);
    }

    @Override
    public Rule update(Rule rule) {
        if (!rules.containsKey(rule.getId())) {
            throw new IllegalArgumentException("Rule not found: " + rule.getId());
        }

        rule.setUpdatedAt(Instant.now());
        rules.put(rule.getId(), rule);
        return rule;
    }

    @Override
    public long count() {
        return rules.size();
    }

    /**
     * Clear all rules (for testing)
     */
    public void clear() {
        rules.clear();
    }

    /**
     * Import rules in bulk
     */
    public void importRules(List<Rule> rulesToImport) {
        rulesToImport.forEach(this::save);
    }
}
