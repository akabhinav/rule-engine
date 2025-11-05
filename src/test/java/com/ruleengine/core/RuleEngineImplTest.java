package com.ruleengine.core;

import com.ruleengine.evaluator.AviatorExpressionEvaluator;
import com.ruleengine.evaluator.OperatorRegistry;
import com.ruleengine.evaluator.SimpleExpressionEvaluator;
import com.ruleengine.model.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Rule Engine
 */
class RuleEngineImplTest {

    private RuleEngine ruleEngine;
    private OperatorRegistry operatorRegistry;

    @BeforeEach
    void setUp() {
        operatorRegistry = new OperatorRegistry();
        SimpleExpressionEvaluator simpleEvaluator = new SimpleExpressionEvaluator(operatorRegistry);
        AviatorExpressionEvaluator aviatorEvaluator = new AviatorExpressionEvaluator();
        ActionExecutor actionExecutor = new ActionExecutor();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        ruleEngine = new RuleEngineImpl(
                List.of(simpleEvaluator, aviatorEvaluator),
                actionExecutor,
                meterRegistry
        );
    }

    @Test
    void testSimpleRule_GreaterThan() {
        // Given: A rule that checks if amount > 1000
        Rule rule = Rule.builder()
                .id("test-001")
                .name("Amount Check")
                .conditions(List.of(
                        Condition.builder()
                                .field("transaction.amount")
                                .operator(Condition.Operator.GREATER_THAN)
                                .value(1000)
                                .build()
                ))
                .build();

        RuleContext context = RuleContext.withFact("transaction",
                Map.of("amount", 1500));

        // When: Execute rule
        RuleResult result = ruleEngine.execute(rule, context);

        // Then: Rule should match
        assertTrue(result.isMatched());
        assertEquals(RuleResult.ExecutionStatus.MATCHED, result.getStatus());
        assertNotNull(result.getExecutionTimeMs());
    }

    @Test
    void testSimpleRule_NotMatched() {
        // Given: A rule that checks if amount > 1000
        Rule rule = Rule.builder()
                .id("test-002")
                .name("Amount Check")
                .conditions(List.of(
                        Condition.builder()
                                .field("transaction.amount")
                                .operator(Condition.Operator.GREATER_THAN)
                                .value(1000)
                                .build()
                ))
                .build();

        RuleContext context = RuleContext.withFact("transaction",
                Map.of("amount", 500));

        // When: Execute rule
        RuleResult result = ruleEngine.execute(rule, context);

        // Then: Rule should not match
        assertFalse(result.isMatched());
        assertEquals(RuleResult.ExecutionStatus.NOT_MATCHED, result.getStatus());
    }

    @Test
    void testMultipleConditions_AND() {
        // Given: A rule with multiple AND conditions
        Rule rule = Rule.builder()
                .id("test-003")
                .name("Multi-Condition Check")
                .conditions(List.of(
                        Condition.builder()
                                .field("transaction.amount")
                                .operator(Condition.Operator.GREATER_THAN)
                                .value(1000)
                                .build(),
                        Condition.builder()
                                .field("user.riskScore")
                                .operator(Condition.Operator.LESS_THAN)
                                .value(50)
                                .build()
                ))
                .conditionLogic(Rule.ConditionLogic.AND)
                .build();

        RuleContext context = RuleContext.withFacts(Map.of(
                "transaction", Map.of("amount", 1500),
                "user", Map.of("riskScore", 30)
        ));

        // When: Execute rule
        RuleResult result = ruleEngine.execute(rule, context);

        // Then: Rule should match
        assertTrue(result.isMatched());
    }

    @Test
    void testMultipleConditions_OR() {
        // Given: A rule with multiple OR conditions
        Rule rule = Rule.builder()
                .id("test-004")
                .name("OR Condition Check")
                .conditions(List.of(
                        Condition.builder()
                                .field("transaction.amount")
                                .operator(Condition.Operator.GREATER_THAN)
                                .value(10000)
                                .build(),
                        Condition.builder()
                                .field("user.vip")
                                .operator(Condition.Operator.EQUALS)
                                .value(true)
                                .build()
                ))
                .conditionLogic(Rule.ConditionLogic.OR)
                .build();

        RuleContext context = RuleContext.withFacts(Map.of(
                "transaction", Map.of("amount", 500),
                "user", Map.of("vip", true)
        ));

        // When: Execute rule
        RuleResult result = ruleEngine.execute(rule, context);

        // Then: Rule should match (vip is true)
        assertTrue(result.isMatched());
    }

    @Test
    void testStringOperators() {
        // Given: A rule with string operators
        Rule rule = Rule.builder()
                .id("test-005")
                .name("Email Check")
                .conditions(List.of(
                        Condition.builder()
                                .field("user.email")
                                .operator(Condition.Operator.CONTAINS)
                                .value("@example.com")
                                .build()
                ))
                .build();

        RuleContext context = RuleContext.withFact("user",
                Map.of("email", "john@example.com"));

        // When: Execute rule
        RuleResult result = ruleEngine.execute(rule, context);

        // Then: Rule should match
        assertTrue(result.isMatched());
    }

    @Test
    void testPrioritizedExecution() {
        // Given: Multiple rules with different priorities
        Rule highPriority = Rule.builder()
                .id("high")
                .name("High Priority")
                .priority(100)
                .conditions(List.of())
                .build();

        Rule lowPriority = Rule.builder()
                .id("low")
                .name("Low Priority")
                .priority(10)
                .conditions(List.of())
                .build();

        List<Rule> rules = List.of(lowPriority, highPriority);
        RuleContext context = new RuleContext();

        // When: Execute prioritized
        List<RuleResult> results = ruleEngine.executePrioritized(rules, context);

        // Then: High priority should execute first
        assertEquals("high", results.get(0).getRuleId());
        assertEquals("low", results.get(1).getRuleId());
    }

    @Test
    void testRuleWithAction() {
        // Given: A rule with actions
        Rule rule = Rule.builder()
                .id("test-006")
                .name("Rule with Action")
                .conditions(List.of(
                        Condition.builder()
                                .field("transaction.amount")
                                .operator(Condition.Operator.GREATER_THAN)
                                .value(1000)
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .id("action-1")
                                .type(Action.ActionType.SET_FACT)
                                .factKey("approved")
                                .factValue(true)
                                .build()
                ))
                .build();

        RuleContext context = RuleContext.withFact("transaction",
                Map.of("amount", 1500));

        // When: Execute rule
        RuleResult result = ruleEngine.execute(rule, context);

        // Then: Action should be executed
        assertTrue(result.isMatched());
        assertFalse(result.getActionResults().isEmpty());
        assertEquals(true, context.getIntermediateResult("approved"));
    }

    @Test
    void testExpressionEvaluation() {
        // Given: A rule with expression
        Rule rule = Rule.builder()
                .id("test-007")
                .name("Expression Rule")
                .conditions(List.of(
                        Condition.builder()
                                .expression("transaction.amount > 1000 && user.riskScore < 50")
                                .build()
                ))
                .build();

        RuleContext context = RuleContext.withFacts(Map.of(
                "transaction", Map.of("amount", 1500),
                "user", Map.of("riskScore", 30)
        ));

        // When: Execute rule
        RuleResult result = ruleEngine.execute(rule, context);

        // Then: Rule should match
        assertTrue(result.isMatched());
    }

    @Test
    void testContextMatching() {
        // Given: A rule with context constraints
        Rule rule = Rule.builder()
                .id("test-008")
                .name("Context-Aware Rule")
                .contextConstraints(Map.of("channel", "mobile"))
                .conditions(List.of())
                .build();

        RuleContext matchingContext = new RuleContext();
        matchingContext.setContextAttribute("channel", "mobile");

        RuleContext nonMatchingContext = new RuleContext();
        nonMatchingContext.setContextAttribute("channel", "web");

        // When: Execute rule
        RuleResult matchingResult = ruleEngine.execute(rule, matchingContext);
        RuleResult nonMatchingResult = ruleEngine.execute(rule, nonMatchingContext);

        // Then
        assertTrue(matchingResult.isMatched());
        assertEquals(RuleResult.ExecutionStatus.SKIPPED, nonMatchingResult.getStatus());
    }

    @Test
    void testStatistics() {
        // Given: Execute some rules
        Rule rule = Rule.builder()
                .id("test-009")
                .name("Stats Test")
                .conditions(List.of())
                .build();

        RuleContext context = new RuleContext();

        // When: Execute multiple times
        for (int i = 0; i < 5; i++) {
            ruleEngine.execute(rule, context);
        }

        // Then: Statistics should be updated
        RuleEngine.EngineStatistics stats = ruleEngine.getStatistics();
        assertEquals(5, stats.totalExecutions());
        assertEquals(5, stats.totalMatches());
    }
}
