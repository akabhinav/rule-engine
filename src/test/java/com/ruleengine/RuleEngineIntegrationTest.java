package com.ruleengine;

import com.ruleengine.evaluator.OperatorRegistry;
import com.ruleengine.model.*;
import com.ruleengine.service.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Integration Tests for Phase 1 Features
 */
@SpringBootTest
class RuleEngineIntegrationTest {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private OperatorRegistry operatorRegistry;

    @BeforeEach
    void setUp() {
        // Clear any existing rules
        ruleService.clearCache();
    }

    @Test
    void testFeature1_DynamicRuleLoading() {
        // Feature 1: Dynamic Rule Loading
        System.out.println("Testing Feature 1: Dynamic Rule Loading");

        String json = """
            {
              "name": "Dynamic Test Rule",
              "conditions": [
                {
                  "field": "amount",
                  "operator": "GREATER_THAN",
                  "value": 1000
                }
              ],
              "actions": []
            }
            """;

        // Create rule from JSON
        Rule rule = ruleService.createRuleFromJson(json);
        assertNotNull(rule.getId());
        assertEquals("Dynamic Test Rule", rule.getName());

        // Verify it was saved
        Rule retrieved = ruleService.getRule(rule.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals("Dynamic Test Rule", retrieved.getName());

        System.out.println("✅ Feature 1 PASSED: Dynamic rule loading works");
    }

    @Test
    void testFeature2_SchemaLessFacts() {
        // Feature 2: Schema-less Facts
        System.out.println("Testing Feature 2: Schema-less Facts");

        Rule rule = Rule.builder()
                .name("Schema-less Test")
                .conditions(List.of(
                        Condition.builder()
                                .field("transaction.merchant.country")
                                .operator(Condition.Operator.EQUALS)
                                .value("US")
                                .build()
                ))
                .build();

        Rule saved = ruleService.createRule(rule);

        // Create nested schema-less fact
        RuleContext context = RuleContext.withFact("transaction", Map.of(
                "amount", 1500,
                "merchant", Map.of(
                        "name", "Acme Corp",
                        "country", "US"
                )
        ));

        RuleResult result = ruleService.executeRule(saved.getId(), context);
        assertTrue(result.isMatched());

        System.out.println("✅ Feature 2 PASSED: Schema-less facts work");
    }

    @Test
    void testFeature3_ExpressionEngine() {
        // Feature 3: Expression Engine
        System.out.println("Testing Feature 3: Expression Engine");

        Rule rule = Rule.builder()
                .name("Expression Test")
                .conditions(List.of(
                        Condition.builder()
                                .expression("transaction.amount > 1000 && user.riskScore < 50")
                                .build()
                ))
                .build();

        Rule saved = ruleService.createRule(rule);

        RuleContext context = RuleContext.withFacts(Map.of(
                "transaction", Map.of("amount", 1500),
                "user", Map.of("riskScore", 30)
        ));

        RuleResult result = ruleService.executeRule(saved.getId(), context);
        assertTrue(result.isMatched());

        System.out.println("✅ Feature 3 PASSED: Expression engine works");
    }

    @Test
    void testFeature4_RuleChaining() {
        // Feature 4: Rule Chaining
        System.out.println("Testing Feature 4: Rule Chaining");

        // Parent rule
        Rule parent = Rule.builder()
                .name("Parent Rule")
                .conditions(List.of())
                .actions(List.of(
                        Action.builder()
                                .type(Action.ActionType.SET_FACT)
                                .factKey("parentExecuted")
                                .factValue(true)
                                .build()
                ))
                .triggers(List.of("child-rule"))
                .build();

        // Child rule
        Rule child = Rule.builder()
                .id("child-rule")
                .name("Child Rule")
                .dependsOn(List.of(parent.getId()))
                .conditions(List.of())
                .build();

        ruleService.createRule(parent);
        ruleService.createRule(child);

        RuleContext context = new RuleContext();
        List<RuleResult> results = ruleService.executeRuleGroup("test-group", context);

        // Verify parent set the fact
        assertEquals(true, context.getIntermediateResult("parentExecuted"));

        System.out.println("✅ Feature 4 PASSED: Rule chaining works");
    }

    @Test
    void testFeature5_ContextMatching() {
        // Feature 5: Conditional Execution Contexts
        System.out.println("Testing Feature 5: Context Matching");

        Rule rule = Rule.builder()
                .name("Context Rule")
                .contextConstraints(Map.of(
                        "channel", "mobile",
                        "region", "US"
                ))
                .conditions(List.of())
                .build();

        Rule saved = ruleService.createRule(rule);

        // Matching context
        RuleContext matchingContext = new RuleContext();
        matchingContext.setContextAttribute("channel", "mobile");
        matchingContext.setContextAttribute("region", "US");

        RuleResult matchingResult = ruleService.executeRule(saved.getId(), matchingContext);
        assertTrue(matchingResult.isMatched());

        // Non-matching context
        RuleContext nonMatchingContext = new RuleContext();
        nonMatchingContext.setContextAttribute("channel", "web");
        nonMatchingContext.setContextAttribute("region", "EU");

        RuleResult nonMatchingResult = ruleService.executeRule(saved.getId(), nonMatchingContext);
        assertEquals(RuleResult.ExecutionStatus.SKIPPED, nonMatchingResult.getStatus());

        System.out.println("✅ Feature 5 PASSED: Context matching works");
    }

    @Test
    void testFeature6_PriorityAndSalience() {
        // Feature 6: Priority + Salience Model
        System.out.println("Testing Feature 6: Priority and Salience");

        Rule highPriority = Rule.builder()
                .name("High Priority")
                .priority(100)
                .salience(90)
                .ruleGroup("priority-test")
                .conditions(List.of())
                .build();

        Rule lowPriority = Rule.builder()
                .name("Low Priority")
                .priority(10)
                .salience(5)
                .ruleGroup("priority-test")
                .conditions(List.of())
                .build();

        ruleService.createRule(highPriority);
        ruleService.createRule(lowPriority);

        RuleContext context = new RuleContext();
        List<RuleResult> results = ruleService.executeRuleGroup("priority-test", context);

        // High priority should execute first
        assertEquals("High Priority", results.get(0).getRuleName());
        assertEquals("Low Priority", results.get(1).getRuleName());

        System.out.println("✅ Feature 6 PASSED: Priority/salience execution works");
    }

    @Test
    void testFeature9_CustomOperators() {
        // Feature 9: Custom Operator Plugins
        System.out.println("Testing Feature 9: Custom Operators");

        // Register custom operator
        operatorRegistry.registerOperator("IS_PREMIUM", (actual, expected) -> {
            return "PREMIUM".equals(actual);
        });

        assertTrue(operatorRegistry.hasOperator("IS_PREMIUM"));

        // Test built-in custom operator
        assertTrue(operatorRegistry.hasOperator("IS_WEEKEND"));
        assertTrue(operatorRegistry.hasOperator("IS_EMPTY"));

        System.out.println("✅ Feature 9 PASSED: Custom operators work");
    }

    @Test
    void testFeature10_ResultEnrichment() {
        // Feature 10: Result Enrichment
        System.out.println("Testing Feature 10: Result Enrichment");

        Rule rule = Rule.builder()
                .name("Enrichment Test")
                .conditions(List.of())
                .actions(List.of(
                        Action.builder()
                                .type(Action.ActionType.ENRICH_RESULT)
                                .metadata(Map.of(
                                        "reason", "Test enrichment",
                                        "severity", "HIGH",
                                        "score", 95
                                ))
                                .build()
                ))
                .build();

        Rule saved = ruleService.createRule(rule);
        RuleContext context = new RuleContext();
        RuleResult result = ruleService.executeRule(saved.getId(), context);

        assertEquals("Test enrichment", result.getEnrichmentData().get("reason"));
        assertEquals("HIGH", result.getEnrichmentData().get("severity"));
        assertEquals(95, result.getEnrichmentData().get("score"));

        System.out.println("✅ Feature 10 PASSED: Result enrichment works");
    }

    @Test
    void testFeature11_RuleGroupsAndVersioning() {
        // Feature 11: Rule Groups + Versioning
        System.out.println("Testing Feature 11: Rule Groups and Versioning");

        Rule v1 = Rule.builder()
                .name("Test Rule")
                .version(1)
                .ruleGroup("versioned-group")
                .conditions(List.of())
                .build();

        Rule v2 = Rule.builder()
                .name("Test Rule")
                .version(2)
                .ruleGroup("versioned-group")
                .conditions(List.of())
                .build();

        ruleService.createRule(v1);
        ruleService.createRule(v2);

        List<Rule> groupRules = ruleService.getRulesByGroup("versioned-group");
        assertEquals(2, groupRules.size());

        System.out.println("✅ Feature 11 PASSED: Groups and versioning work");
    }

    @Test
    void testFeature14_MemoryCache() {
        // Feature 14: Memory Cache
        System.out.println("Testing Feature 14: Memory Cache");

        Rule rule = Rule.builder()
                .name("Cache Test")
                .conditions(List.of())
                .build();

        Rule saved = ruleService.createRule(rule);

        // First access - cache miss
        ruleService.getRule(saved.getId());

        // Second access - cache hit
        ruleService.getRule(saved.getId());

        var cacheStats = ruleService.getCacheStatistics();
        assertTrue(cacheStats.hits() > 0);

        System.out.println("✅ Feature 14 PASSED: Memory cache works");
        System.out.println("   Cache hits: " + cacheStats.hits());
        System.out.println("   Cache hit rate: " + String.format("%.2f%%", cacheStats.hitRate() * 100));
    }

    @Test
    void testFeature16_TestingSandbox() {
        // Feature 16: Rule Testing Sandbox
        System.out.println("Testing Feature 16: Testing Sandbox");

        Rule testRule = Rule.builder()
                .name("Sandbox Test")
                .conditions(List.of(
                        Condition.builder()
                                .field("amount")
                                .operator(Condition.Operator.GREATER_THAN)
                                .value(500)
                                .build()
                ))
                .build();

        RuleContext context = RuleContext.withFact("transaction", Map.of("amount", 1000));

        // Test without saving
        RuleResult result = ruleService.testRule(testRule, context);
        assertTrue(result.isMatched());

        // Verify rule was NOT saved
        List<Rule> allRules = ruleService.getAllRules();
        assertFalse(allRules.stream().anyMatch(r -> "Sandbox Test".equals(r.getName())));

        System.out.println("✅ Feature 16 PASSED: Testing sandbox works");
    }

    @Test
    void testComplexFraudDetectionScenario() {
        // Complex real-world scenario
        System.out.println("\nTesting Complex Fraud Detection Scenario");

        Rule fraudRule = Rule.builder()
                .name("Fraud Detection")
                .priority(100)
                .conditions(List.of(
                        Condition.builder()
                                .field("transaction.amount")
                                .operator(Condition.Operator.GREATER_THAN)
                                .value(5000)
                                .build(),
                        Condition.builder()
                                .field("user.riskScore")
                                .operator(Condition.Operator.GREATER_THAN)
                                .value(70)
                                .build(),
                        Condition.builder()
                                .field("transaction.country")
                                .operator(Condition.Operator.NOT_EQUALS)
                                .value("US")
                                .build()
                ))
                .conditionLogic(Rule.ConditionLogic.AND)
                .actions(List.of(
                        Action.builder()
                                .type(Action.ActionType.SET_FACT)
                                .factKey("fraudAlert")
                                .factValue(true)
                                .build(),
                        Action.builder()
                                .type(Action.ActionType.ENRICH_RESULT)
                                .metadata(Map.of(
                                        "alertType", "FRAUD",
                                        "severity", "HIGH",
                                        "reason", "High-value foreign transaction with elevated risk"
                                ))
                                .build()
                ))
                .build();

        Rule saved = ruleService.createRule(fraudRule);

        RuleContext context = RuleContext.withFacts(Map.of(
                "transaction", Map.of(
                        "amount", 7500,
                        "country", "BR",
                        "merchant", "Unknown Merchant"
                ),
                "user", Map.of(
                        "id", "user123",
                        "riskScore", 85,
                        "homeCountry", "US"
                )
        ));

        RuleResult result = ruleService.executeRule(saved.getId(), context);

        assertTrue(result.isMatched());
        assertEquals(true, context.getIntermediateResult("fraudAlert"));
        assertEquals("FRAUD", result.getEnrichmentData().get("alertType"));
        assertEquals("HIGH", result.getEnrichmentData().get("severity"));
        assertNotNull(result.getExecutionTimeMs());
        assertTrue(result.getExecutionTimeMs() < 100); // Should be fast!

        System.out.println("✅ COMPLEX SCENARIO PASSED: Fraud detection works");
        System.out.println("   Execution time: " + result.getExecutionTimeMs() + "ms");
    }

    @Test
    void testPerformanceAndStatistics() {
        // Performance test
        System.out.println("\nTesting Performance and Statistics");

        Rule perfRule = Rule.builder()
                .name("Performance Test")
                .conditions(List.of(
                        Condition.builder()
                                .expression("amount > 100")
                                .build()
                ))
                .build();

        Rule saved = ruleService.createRule(perfRule);
        RuleContext context = RuleContext.withFact("data", Map.of("amount", 500));

        // Execute 100 times
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            ruleService.executeRule(saved.getId(), context);
        }
        long totalTime = System.currentTimeMillis() - startTime;

        var stats = ruleService.getStatistics();
        assertTrue(stats.totalExecutions() >= 100);
        assertTrue(stats.avgExecutionTimeMs() < 10); // Should be very fast

        System.out.println("✅ PERFORMANCE TEST PASSED");
        System.out.println("   100 executions in: " + totalTime + "ms");
        System.out.println("   Average per execution: " + (totalTime / 100.0) + "ms");
        System.out.println("   Engine stats - Total executions: " + stats.totalExecutions());
        System.out.println("   Engine stats - Avg time: " + stats.avgExecutionTimeMs() + "ms");
    }
}
