package com.ruleengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Condition Model - Represents a single rule condition
 *
 * Supports:
 * - Simple field comparisons
 * - Complex expressions
 * - Nested conditions
 * - Custom operators
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Condition {

    private String id;

    // Simple Condition (field-operator-value)
    private String field; // e.g., "transaction.amount"
    private Operator operator; // e.g., GREATER_THAN
    private Object value; // e.g., 1000

    // Complex Expression (for advanced use cases)
    private String expression; // e.g., "transaction.amount > 1000 && user.riskScore < 50"

    // Nested Conditions
    @Builder.Default
    private List<Condition> nestedConditions = new ArrayList<>();
    private LogicalOperator logicalOperator; // AND, OR for nested conditions

    // Metadata
    private String description;

    public enum Operator {
        // Comparison
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN,
        LESS_THAN_OR_EQUALS,

        // String
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        MATCHES, // Regex

        // Collection
        IN,
        NOT_IN,

        // Range
        BETWEEN,

        // Null checks
        IS_NULL,
        IS_NOT_NULL,

        // Custom operators (pluggable)
        CUSTOM
    }

    public enum LogicalOperator {
        AND, OR, NOT
    }

    /**
     * Check if this is a simple condition (field-operator-value)
     */
    public boolean isSimple() {
        return field != null && operator != null && expression == null;
    }

    /**
     * Check if this is a complex expression
     */
    public boolean isExpression() {
        return expression != null && !expression.isEmpty();
    }

    /**
     * Check if this has nested conditions
     */
    public boolean isNested() {
        return !nestedConditions.isEmpty();
    }
}
