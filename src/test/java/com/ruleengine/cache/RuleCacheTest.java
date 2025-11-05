package com.ruleengine.cache;

import com.ruleengine.model.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test caching functionality
 */
class RuleCacheTest {

    private RuleCache cache;

    @BeforeEach
    void setUp() {
        cache = new RuleCache();
        cache.clearAll();
    }

    @Test
    void testRuleCaching() {
        Rule rule = Rule.builder()
                .id("test-1")
                .name("Test Rule")
                .build();

        // Put in cache
        cache.putRule(rule.getId(), rule);

        // Retrieve from cache
        var cached = cache.getRule(rule.getId());
        assertTrue(cached.isPresent());
        assertEquals("Test Rule", cached.get().getName());
    }

    @Test
    void testRuleCacheMiss() {
        var cached = cache.getRule("non-existent");
        assertTrue(cached.isEmpty());
    }

    @Test
    void testRuleCacheInvalidation() {
        Rule rule = Rule.builder()
                .id("test-2")
                .name("Test Rule")
                .build();

        cache.putRule(rule.getId(), rule);
        assertTrue(cache.getRule(rule.getId()).isPresent());

        // Invalidate
        cache.invalidateRule(rule.getId());
        assertTrue(cache.getRule(rule.getId()).isEmpty());
    }

    @Test
    void testRuleGroupCaching() {
        List<Rule> rules = List.of(
                Rule.builder().id("r1").name("Rule 1").build(),
                Rule.builder().id("r2").name("Rule 2").build()
        );

        cache.putRuleGroup("group1", rules);

        var cached = cache.getRuleGroup("group1");
        assertTrue(cached.isPresent());
        assertEquals(2, cached.get().size());
    }

    @Test
    void testCompiledExpressionCaching() {
        String expression = "amount > 1000";
        Object compiled = new Object(); // Simulate compiled expression

        cache.putCompiledExpression(expression, compiled);

        var cached = cache.getCompiledExpression(expression);
        assertTrue(cached.isPresent());
        assertSame(compiled, cached.get());
    }

    @Test
    void testCacheStatistics() {
        Rule rule = Rule.builder()
                .id("test-3")
                .name("Stats Test")
                .build();

        // Cache miss
        cache.getRule("test-3");

        // Cache put
        cache.putRule("test-3", rule);

        // Cache hit
        cache.getRule("test-3");

        var stats = cache.getStatistics();
        assertTrue(stats.hits() > 0);
        assertTrue(stats.size() > 0);
    }

    @Test
    void testClearAll() {
        Rule rule = Rule.builder()
                .id("test-4")
                .name("Clear Test")
                .build();

        cache.putRule(rule.getId(), rule);
        cache.putRuleGroup("group", List.of(rule));
        cache.putCompiledExpression("expr", new Object());

        // Clear all
        cache.clearAll();

        assertTrue(cache.getRule(rule.getId()).isEmpty());
        assertTrue(cache.getRuleGroup("group").isEmpty());
        assertTrue(cache.getCompiledExpression("expr").isEmpty());
    }
}
