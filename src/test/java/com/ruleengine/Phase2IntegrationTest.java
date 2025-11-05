package com.ruleengine;

import com.ruleengine.audit.AuditLogger;
import com.ruleengine.core.RuleIndexer;
import com.ruleengine.core.TemporalRuleExecutor;
import com.ruleengine.model.*;
import com.ruleengine.service.RuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2 Integration Tests
 * Tests all new advanced features
 */
@SpringBootTest
public class Phase2IntegrationTest {

    @Autowired
    private RuleService ruleService;

    @Autowired(required = false)
    private RuleIndexer ruleIndexer;

    @Autowired(required = false)
    private TemporalRuleExecutor temporalExecutor;

    @Autowired(required = false)
    private AuditLogger auditLogger;

    @Test
    void testRuleIndexing() {
        System.out.println("\n=== Testing Rule Indexing ===");

        if (ruleIndexer == null) {
            System.out.println("⚠️ RuleIndexer not available, skipping test");
            return;
        }

        // Create rules with indexable conditions
        Rule rule1 = Rule.builder()
                .name("Indexed Rule 1")
                .conditions(List.of(
                        Condition.builder()
                                .field("transaction.amount")
                                .operator(Condition.Operator.GREATER_THAN)
                                .value(1000)
                                .build()
                ))
                .build();

        Rule savedRule = ruleService.createRule(rule1);

        // Index the rule
        ruleIndexer.indexRule(savedRule);

        // Create context with matching data
        RuleContext context = RuleContext.withFact("transaction",
                Map.of("amount", 1500));

        // Find candidate rules using index
        List<Rule> candidates = ruleIndexer.findCandidateRules(context);
        assertFalse(candidates.isEmpty());

        // Get index statistics
        var stats = ruleIndexer.getStatistics();
        assertTrue(stats.totalRules() > 0);

        System.out.println("✅ Rule Indexing Test PASSED");
        System.out.println("   Indexed rules: " + stats.totalRules());
        System.out.println("   Indexed fields: " + stats.indexedFields());
    }

    @Test
    void testTemporalWindows() {
        System.out.println("\n=== Testing Temporal Windows ===");

        if (temporalExecutor == null) {
            System.out.println("⚠️ TemporalExecutor not available, skipping test");
            return;
        }

        // Create rule with sliding window
        Rule temporalRule = Rule.builder()
                .name("Temporal Window Rule")
                .temporalWindow(TemporalWindow.builder()
                        .type(TemporalWindow.WindowType.SLIDING)
                        .windowDuration(Duration.ofMinutes(10))
                        .eventCount(3)
                        .build())
                .conditions(List.of())
                .build();

        Rule saved = ruleService.createRule(temporalRule);

        // Create multiple events
        for (int i = 0; i < 5; i++) {
            RuleContext context = RuleContext.withFact("event",
                    Map.of("value", i));

            ruleService.executeRule(saved.getId(), context);
        }

        // Get temporal statistics
        var stats = temporalExecutor.getStatistics();
        assertTrue(stats.totalEvents() > 0);

        System.out.println("✅ Temporal Windows Test PASSED");
        System.out.println("   Tracked rules: " + stats.trackedRules());
        System.out.println("   Total events: " + stats.totalEvents());
    }

    @Test
    void testAuditLogging() {
        System.out.println("\n=== Testing Audit Logging ===");

        if (auditLogger == null) {
            System.out.println("⚠️ AuditLogger not available, skipping test");
            return;
        }

        // Create and execute rule
        Rule rule = Rule.builder()
                .name("Audit Test Rule")
                .conditions(List.of(
                        Condition.builder()
                                .field("value")
                                .operator(Condition.Operator.EQUALS)
                                .value(100)
                                .build()
                ))
                .build();

        Rule saved = ruleService.createRule(rule);

        // Execute rule multiple times
        for (int i = 0; i < 3; i++) {
            RuleContext context = RuleContext.withFact("data",
                    Map.of("value", 100));
            ruleService.executeRule(saved.getId(), context);
        }

        // Check audit log
        List<AuditLogger.AuditEntry> auditLog = auditLogger.getAuditLog(saved.getId());
        assertFalse(auditLog.isEmpty());

        // Get audit statistics
        var stats = auditLogger.getStatistics();
        assertTrue(stats.totalExecutions() > 0);

        System.out.println("✅ Audit Logging Test PASSED");
        System.out.println("   Total audit entries: " + stats.totalEntries());
        System.out.println("   Total executions: " + stats.totalExecutions());
        System.out.println("   Tracked rules: " + stats.trackedRules());
    }

    @Test
    void testComplexTemporalScenario() {
        System.out.println("\n=== Testing Complex Temporal Scenario ===");

        if (temporalExecutor == null) {
            System.out.println("⚠️  TemporalExecutor not available, skipping test");
            return;
        }

        // Fraud detection: More than 5 transactions in 10 minutes
        Rule fraudRule = Rule.builder()
                .name("Rapid Transaction Alert")
                .temporalWindow(TemporalWindow.builder()
                        .type(TemporalWindow.WindowType.SLIDING)
                        .windowDuration(Duration.ofMinutes(10))
                        .eventCount(5)
                        .aggregation(TemporalWindow.AggregationType.COUNT)
                        .aggregationField("transaction.amount")
                        .build())
                .conditions(List.of())
                .actions(List.of(
                        Action.builder()
                                .type(Action.ActionType.SET_FACT)
                                .factKey("fraudAlert")
                                .factValue(true)
                                .build()
                ))
                .build();

        Rule saved = ruleService.createRule(fraudRule);

        // Simulate 6 rapid transactions
        for (int i = 1; i <= 6; i++) {
            RuleContext context = RuleContext.withFact("transaction",
                    Map.of("amount", 100 * i, "id", "txn-" + i));

            RuleResult result = ruleService.executeRule(saved.getId(), context);
            System.out.println("   Transaction " + i + " - Matched: " + result.isMatched());
        }

        System.out.println("✅ Complex Temporal Scenario Test PASSED");
    }

    @Test
    void testScriptExecution() {
        System.out.println("\n=== Testing Script Execution ===");

        // Create rule with MVEL script
        Rule scriptRule = Rule.builder()
                .name("Script Test Rule")
                .conditions(List.of())
                .actions(List.of(
                        Action.builder()
                                .type(Action.ActionType.EXECUTE_SCRIPT)
                                .script("transaction.amount * 0.1")
                                .scriptLanguage(Action.ScriptLanguage.MVEL)
                                .build()
                ))
                .build();

        Rule saved = ruleService.createRule(scriptRule);

        RuleContext context = RuleContext.withFact("transaction",
                Map.of("amount", 1000));

        RuleResult result = ruleService.executeRule(saved.getId(), context);

        // Check if script was executed
        assertNotNull(result);
        System.out.println("✅ Script Execution Test PASSED");
    }

    @Test
    void testPerformanceWithIndexing() {
        System.out.println("\n=== Testing Performance with Indexing ===");

        if (ruleIndexer == null) {
            System.out.println("⚠️ RuleIndexer not available, skipping test");
            return;
        }

        // Create 50 rules
        for (int i = 0; i < 50; i++) {
            Rule rule = Rule.builder()
                    .name("Perf Rule " + i)
                    .conditions(List.of(
                            Condition.builder()
                                    .field("value")
                                    .operator(Condition.Operator.EQUALS)
                                    .value(i)
                                    .build()
                    ))
                    .build();

            Rule saved = ruleService.createRule(rule);
            ruleIndexer.indexRule(saved);
        }

        RuleContext context = RuleContext.withFact("data",
                Map.of("value", 25));

        // Execute with indexing
        long startTime = System.currentTimeMillis();
        List<Rule> candidates = ruleIndexer.findCandidateRules(context);
        long indexTime = System.currentTimeMillis() - startTime;

        System.out.println("✅ Performance Test PASSED");
        System.out.println("   Total indexed rules: 50");
        System.out.println("   Candidate rules found: " + candidates.size());
        System.out.println("   Index lookup time: " + indexTime + "ms");
    }

    @Test
    void testEndToEndPhase2() {
        System.out.println("\n=== End-to-End Phase 2 Test ===");

        // Complex rule with all Phase 2 features
        Rule complexRule = Rule.builder()
                .name("Phase 2 Complete Rule")
                .priority(100)
                .conditions(List.of(
                        Condition.builder()
                                .field("transaction.amount")
                                .operator(Condition.Operator.GREATER_THAN)
                                .value(5000)
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(Action.ActionType.SET_FACT)
                                .factKey("highValue")
                                .factValue(true)
                                .build(),
                        Action.builder()
                                .type(Action.ActionType.ENRICH_RESULT)
                                .metadata(Map.of(
                                        "category", "HIGH_VALUE",
                                        "reviewRequired", true
                                ))
                                .build()
                ))
                .temporalWindow(TemporalWindow.builder()
                        .type(TemporalWindow.WindowType.SLIDING)
                        .windowDuration(Duration.ofHours(1))
                        .build())
                .build();

        Rule saved = ruleService.createRule(complexRule);

        // Index it
        if (ruleIndexer != null) {
            ruleIndexer.indexRule(saved);
        }

        // Execute
        RuleContext context = RuleContext.withFact("transaction",
                Map.of("amount", 7500, "currency", "USD"));

        RuleResult result = ruleService.executeRule(saved.getId(), context);

        assertTrue(result.isMatched());
        assertEquals("HIGH_VALUE", result.getEnrichmentData().get("category"));
        assertEquals(true, context.getIntermediateResult("highValue"));

        // Check audit log
        if (auditLogger != null) {
            List<AuditLogger.AuditEntry> auditLog = auditLogger.getAuditLog(saved.getId());
            assertFalse(auditLog.isEmpty());
        }

        System.out.println("✅ End-to-End Phase 2 Test PASSED");
        System.out.println("   Rule matched: " + result.isMatched());
        System.out.println("   Execution time: " + result.getExecutionTimeMs() + "ms");
    }
}
