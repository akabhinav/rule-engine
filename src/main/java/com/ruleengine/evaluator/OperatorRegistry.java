package com.ruleengine.evaluator;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Operator Registry - Pluggable custom operators
 *
 * Allows users to register custom operators like:
 * - isWeekend()
 * - isHoliday()
 * - withinRadius()
 * - trendUp()
 * - etc.
 */
@Component
public class OperatorRegistry {

    private final Map<String, CustomOperator> operators = new ConcurrentHashMap<>();

    public OperatorRegistry() {
        // Register built-in custom operators
        registerBuiltInOperators();
    }

    /**
     * Register a custom operator
     */
    public void registerOperator(String name, CustomOperator operator) {
        operators.put(name.toUpperCase(), operator);
    }

    /**
     * Register a custom operator with lambda
     */
    public void registerOperator(String name, BiFunction<Object, Object, Boolean> evaluator) {
        operators.put(name.toUpperCase(), new CustomOperator() {
            @Override
            public boolean evaluate(Object actual, Object expected) {
                return evaluator.apply(actual, expected);
            }

            @Override
            public String getName() {
                return name;
            }
        });
    }

    /**
     * Evaluate using custom operator
     */
    public boolean evaluate(String operatorName, Object actual, Object expected) {
        CustomOperator operator = operators.get(operatorName.toUpperCase());
        if (operator == null) {
            throw new IllegalArgumentException("Unknown operator: " + operatorName);
        }
        return operator.evaluate(actual, expected);
    }

    /**
     * Check if operator exists
     */
    public boolean hasOperator(String name) {
        return operators.containsKey(name.toUpperCase());
    }

    /**
     * Get all registered operators
     */
    public Map<String, CustomOperator> getAllOperators() {
        return new ConcurrentHashMap<>(operators);
    }

    /**
     * Register built-in custom operators
     */
    private void registerBuiltInOperators() {
        // IS_WEEKEND - check if date is weekend
        registerOperator("IS_WEEKEND", (actual, expected) -> {
            if (actual instanceof java.time.DayOfWeek) {
                java.time.DayOfWeek day = (java.time.DayOfWeek) actual;
                return day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY;
            }
            return false;
        });

        // WITHIN_RANGE - check if number is within percentage range
        registerOperator("WITHIN_RANGE", (actual, expected) -> {
            if (actual instanceof Number && expected instanceof Number) {
                double a = ((Number) actual).doubleValue();
                double e = ((Number) expected).doubleValue();
                double diff = Math.abs(a - e);
                double percent = (diff / e) * 100;
                return percent <= 10; // Within 10%
            }
            return false;
        });

        // HAS_PATTERN - advanced pattern matching
        registerOperator("HAS_PATTERN", (actual, expected) -> {
            if (actual == null || expected == null) {
                return false;
            }
            String pattern = expected.toString();
            String value = actual.toString();
            return value.matches(pattern);
        });

        // SIZE_EQUALS - check collection/array/string size
        registerOperator("SIZE_EQUALS", (actual, expected) -> {
            if (expected instanceof Number) {
                int expectedSize = ((Number) expected).intValue();
                if (actual instanceof String) {
                    return ((String) actual).length() == expectedSize;
                } else if (actual instanceof java.util.Collection) {
                    return ((java.util.Collection<?>) actual).size() == expectedSize;
                } else if (actual instanceof Map) {
                    return ((Map<?, ?>) actual).size() == expectedSize;
                }
            }
            return false;
        });

        // IS_EMPTY - check if collection/string is empty
        registerOperator("IS_EMPTY", (actual, expected) -> {
            if (actual instanceof String) {
                return ((String) actual).isEmpty();
            } else if (actual instanceof java.util.Collection) {
                return ((java.util.Collection<?>) actual).isEmpty();
            } else if (actual instanceof Map) {
                return ((Map<?, ?>) actual).isEmpty();
            }
            return actual == null;
        });
    }

    /**
     * Custom Operator Interface
     */
    public interface CustomOperator {
        boolean evaluate(Object actual, Object expected);
        String getName();
    }
}
