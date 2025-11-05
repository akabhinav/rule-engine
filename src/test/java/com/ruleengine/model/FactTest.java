package com.ruleengine.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for schema-less Fact model
 */
class FactTest {

    @Test
    void testSimpleFieldAccess() {
        // Given
        Fact fact = new Fact("transaction", Map.of(
                "amount", 1000,
                "currency", "USD"
        ));

        // When/Then
        assertEquals(1000, fact.getValue("amount"));
        assertEquals("USD", fact.getValue("currency"));
    }

    @Test
    void testNestedFieldAccess() {
        // Given
        Map<String, Object> user = new HashMap<>();
        user.put("name", "John");
        user.put("profile", Map.of(
                "age", 30,
                "city", "New York"
        ));

        Fact fact = new Fact("user", user);

        // When/Then
        assertEquals("John", fact.getValue("name"));
        assertEquals(30, fact.getValue("profile.age"));
        assertEquals("New York", fact.getValue("profile.city"));
    }

    @Test
    void testDeeplyNestedAccess() {
        // Given
        Map<String, Object> data = Map.of(
                "transaction", Map.of(
                        "details", Map.of(
                                "merchant", Map.of(
                                        "name", "Acme Corp",
                                        "country", "US"
                                )
                        )
                )
        );

        Fact fact = new Fact("payment", data);

        // When/Then
        assertEquals("Acme Corp", fact.getValue("transaction.details.merchant.name"));
        assertEquals("US", fact.getValue("transaction.details.merchant.country"));
    }

    @Test
    void testTypeSafeGetters() {
        // Given
        Fact fact = new Fact("data", Map.of(
                "count", 42,
                "price", 99.99,
                "active", true,
                "name", "Product"
        ));

        // When/Then
        assertEquals(42, fact.getAsInteger("count"));
        assertEquals(99.99, fact.getAsDouble("price"), 0.01);
        assertEquals(true, fact.getAsBoolean("active"));
        assertEquals("Product", fact.getAsString("name"));
    }

    @Test
    void testSetValue() {
        // Given
        Fact fact = new Fact("data", new HashMap<>());

        // When
        fact.setValue("user.name", "Alice");
        fact.setValue("user.age", 25);

        // Then
        assertEquals("Alice", fact.getValue("user.name"));
        assertEquals(25, fact.getValue("user.age"));
    }

    @Test
    void testMergeFacts() {
        // Given
        Fact fact1 = new Fact("data", new HashMap<>(Map.of("a", 1, "b", 2)));
        Fact fact2 = new Fact("data", Map.of("c", 3, "d", 4));

        // When
        fact1.merge(fact2);

        // Then
        assertEquals(1, fact1.getValue("a"));
        assertEquals(2, fact1.getValue("b"));
        assertEquals(3, fact1.getValue("c"));
        assertEquals(4, fact1.getValue("d"));
    }

    @Test
    void testNullValues() {
        // Given
        Fact fact = new Fact("data", Map.of("exists", "value"));

        // When/Then
        assertNull(fact.getValue("nonexistent"));
        assertNull(fact.getValue("nonexistent.nested"));
        assertNotNull(fact.getValue("exists"));
    }
}
