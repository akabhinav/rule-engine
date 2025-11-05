package com.ruleengine.evaluator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test all custom operators
 */
class OperatorRegistryTest {

    private OperatorRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new OperatorRegistry();
    }

    @Test
    void testBuiltInOperator_IsWeekend() {
        assertTrue(registry.hasOperator("IS_WEEKEND"));

        assertTrue(registry.evaluate("IS_WEEKEND", DayOfWeek.SATURDAY, null));
        assertTrue(registry.evaluate("IS_WEEKEND", DayOfWeek.SUNDAY, null));
        assertFalse(registry.evaluate("IS_WEEKEND", DayOfWeek.MONDAY, null));
    }

    @Test
    void testBuiltInOperator_WithinRange() {
        assertTrue(registry.hasOperator("WITHIN_RANGE"));

        // 105 is within 10% of 100
        assertTrue(registry.evaluate("WITHIN_RANGE", 105, 100));

        // 95 is within 10% of 100
        assertTrue(registry.evaluate("WITHIN_RANGE", 95, 100));

        // 120 is NOT within 10% of 100
        assertFalse(registry.evaluate("WITHIN_RANGE", 120, 100));
    }

    @Test
    void testBuiltInOperator_SizeEquals() {
        assertTrue(registry.hasOperator("SIZE_EQUALS"));

        assertEquals(true, registry.evaluate("SIZE_EQUALS", "hello", 5));
        assertEquals(true, registry.evaluate("SIZE_EQUALS", List.of(1, 2, 3), 3));
        assertEquals(true, registry.evaluate("SIZE_EQUALS", Map.of("a", 1, "b", 2), 2));
        assertEquals(false, registry.evaluate("SIZE_EQUALS", "hello", 3));
    }

    @Test
    void testBuiltInOperator_IsEmpty() {
        assertTrue(registry.hasOperator("IS_EMPTY"));

        assertTrue(registry.evaluate("IS_EMPTY", "", null));
        assertTrue(registry.evaluate("IS_EMPTY", List.of(), null));
        assertTrue(registry.evaluate("IS_EMPTY", Map.of(), null));
        assertFalse(registry.evaluate("IS_EMPTY", "text", null));
        assertFalse(registry.evaluate("IS_EMPTY", List.of(1), null));
    }

    @Test
    void testCustomOperatorRegistration() {
        // Register custom operator
        registry.registerOperator("IS_POSITIVE", (actual, expected) -> {
            if (actual instanceof Number) {
                return ((Number) actual).doubleValue() > 0;
            }
            return false;
        });

        assertTrue(registry.hasOperator("IS_POSITIVE"));
        assertTrue(registry.evaluate("IS_POSITIVE", 10, null));
        assertFalse(registry.evaluate("IS_POSITIVE", -5, null));
    }

    @Test
    void testMultipleCustomOperators() {
        // Register multiple operators
        registry.registerOperator("IS_EVEN", (actual, expected) -> {
            if (actual instanceof Integer) {
                return ((Integer) actual) % 2 == 0;
            }
            return false;
        });

        registry.registerOperator("IS_ODD", (actual, expected) -> {
            if (actual instanceof Integer) {
                return ((Integer) actual) % 2 != 0;
            }
            return false;
        });

        assertTrue(registry.evaluate("IS_EVEN", 4, null));
        assertFalse(registry.evaluate("IS_EVEN", 3, null));

        assertTrue(registry.evaluate("IS_ODD", 3, null));
        assertFalse(registry.evaluate("IS_ODD", 4, null));
    }

    @Test
    void testGetAllOperators() {
        Map<String, OperatorRegistry.CustomOperator> operators = registry.getAllOperators();

        // Should have all built-in operators
        assertTrue(operators.containsKey("IS_WEEKEND"));
        assertTrue(operators.containsKey("WITHIN_RANGE"));
        assertTrue(operators.containsKey("HAS_PATTERN"));
        assertTrue(operators.containsKey("SIZE_EQUALS"));
        assertTrue(operators.containsKey("IS_EMPTY"));
    }

    @Test
    void testUnknownOperator() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.evaluate("UNKNOWN_OPERATOR", "value", "expected");
        });
    }
}
