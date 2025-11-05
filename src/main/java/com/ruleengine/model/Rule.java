package com.ruleengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core Rule Model - Represents a business rule
 *
 * Features:
 * - Dynamic conditions (no compilation needed)
 * - Priority/salience based execution
 * - Versioning support
 * - Rule grouping
 * - Temporal windows
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rule {

    private String id;
    private String name;
    private String description;

    // Version Management
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;

    // Rule Grouping & Organization
    private String ruleGroup;
    private String tenant;

    // Execution Control
    @Builder.Default
    private Integer priority = 0; // Higher priority executes first
    @Builder.Default
    private Integer salience = 0; // Salience for conflict resolution
    @Builder.Default
    private Boolean enabled = true;

    // Conditions
    @Builder.Default
    private List<Condition> conditions = new ArrayList<>();
    private ConditionLogic conditionLogic; // AND, OR, CUSTOM

    // Actions
    @Builder.Default
    private List<Action> actions = new ArrayList<>();

    // Rule Chaining
    @Builder.Default
    private List<String> dependsOn = new ArrayList<>(); // Rule IDs this rule depends on
    @Builder.Default
    private List<String> triggers = new ArrayList<>(); // Rule IDs to trigger after this

    // Context Matching
    @Builder.Default
    private Map<String, Object> contextConstraints = new HashMap<>(); // e.g., {"channel": "mobile", "region": "US"}

    // Temporal Rules
    private TemporalWindow temporalWindow;

    // Metadata
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // Optimization Hints
    private Long executionCount; // Track hot rules for JIT
    private Double avgExecutionTimeMs;

    public enum ConditionLogic {
        AND, OR, CUSTOM
    }

    /**
     * Check if this rule should be executed in the given context
     */
    public boolean matchesContext(RuleContext context) {
        if (contextConstraints.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Object> constraint : contextConstraints.entrySet()) {
            Object contextValue = context.getContextAttribute(constraint.getKey());
            if (!constraint.getValue().equals(contextValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if this is a hot rule (executed frequently)
     */
    public boolean isHotRule(long threshold) {
        return executionCount != null && executionCount >= threshold;
    }
}
