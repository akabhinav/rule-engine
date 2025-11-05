package com.ruleengine.repository;

import com.ruleengine.model.Rule;

import java.util.List;
import java.util.Optional;

/**
 * Rule Repository Interface
 *
 * Manages rule persistence and retrieval
 */
public interface RuleRepository {

    /**
     * Save a rule
     */
    Rule save(Rule rule);

    /**
     * Find rule by ID
     */
    Optional<Rule> findById(String id);

    /**
     * Find all rules
     */
    List<Rule> findAll();

    /**
     * Find rules by group
     */
    List<Rule> findByGroup(String group);

    /**
     * Find rules by tenant
     */
    List<Rule> findByTenant(String tenant);

    /**
     * Find enabled rules
     */
    List<Rule> findEnabled();

    /**
     * Find rule by name and version
     */
    Optional<Rule> findByNameAndVersion(String name, Integer version);

    /**
     * Get latest version of a rule
     */
    Optional<Rule> findLatestVersion(String name);

    /**
     * Delete rule by ID
     */
    void deleteById(String id);

    /**
     * Update rule
     */
    Rule update(Rule rule);

    /**
     * Count total rules
     */
    long count();
}
