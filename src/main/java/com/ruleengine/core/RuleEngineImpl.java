package com.ruleengine.core;

import com.ruleengine.audit.AuditLogger;
import com.ruleengine.evaluator.ExpressionEvaluator;
import com.ruleengine.model.*;
import com.ruleengine.websocket.RuleExecutionNotifier;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Rule Engine Implementation - Phase 2 Enhanced
 *
 * Features:
 * - Dynamic rule execution
 * - Priority-based ordering
 * - Dependency resolution (DAG)
 * - Performance tracking
 * - Rule indexing for O(1) lookup
 * - Temporal window support
 * - Comprehensive audit logging
 * - Concurrent execution support
 */
@Component
public class RuleEngineImpl implements RuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(RuleEngineImpl.class);

    private final List<ExpressionEvaluator> evaluators;
    private final ActionExecutor actionExecutor;
    private final MeterRegistry meterRegistry;

    // Phase 2 Components
    private final RuleIndexer ruleIndexer;
    private final TemporalRuleExecutor temporalExecutor;
    private final AuditLogger auditLogger;
    private final RuleExecutionNotifier executionNotifier;

    // Statistics
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong totalMatches = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final Map<String, Long> executionTimes = new ConcurrentHashMap<>();

    // Metrics
    private final Timer executionTimer;
    private final Counter matchCounter;
    private final Counter errorCounter;

    public RuleEngineImpl(
            List<ExpressionEvaluator> evaluators,
            ActionExecutor actionExecutor,
            MeterRegistry meterRegistry,
            @Autowired(required = false) RuleIndexer ruleIndexer,
            @Autowired(required = false) TemporalRuleExecutor temporalExecutor,
            @Autowired(required = false) AuditLogger auditLogger,
            @Autowired(required = false) RuleExecutionNotifier executionNotifier) {
        this.evaluators = evaluators;
        this.actionExecutor = actionExecutor;
        this.meterRegistry = meterRegistry;
        this.ruleIndexer = ruleIndexer;
        this.temporalExecutor = temporalExecutor;
        this.auditLogger = auditLogger;
        this.executionNotifier = executionNotifier;

        // Initialize metrics
        this.executionTimer = Timer.builder("rule.execution.time")
                .description("Rule execution time")
                .register(meterRegistry);
        this.matchCounter = Counter.builder("rule.matches")
                .description("Number of rule matches")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("rule.errors")
                .description("Number of rule errors")
                .register(meterRegistry);
    }

    @Override
    public RuleResult execute(Rule rule, RuleContext context) {
        totalExecutions.incrementAndGet();

        return executionTimer.record(() -> {
            long startTime = System.currentTimeMillis();
            RuleResult result = executeInternal(rule, context);
            long executionTime = System.currentTimeMillis() - startTime;

            result.setExecutionTimeMs(executionTime);
            executionTimes.put(rule.getId(), executionTime);

            if (result.isMatched()) {
                totalMatches.incrementAndGet();
                matchCounter.increment();
            }

            if (result.getStatus() == RuleResult.ExecutionStatus.ERROR) {
                totalErrors.incrementAndGet();
                errorCounter.increment();
            }

            return result;
        });
    }

    private RuleResult executeInternal(Rule rule, RuleContext context) {
        try {
            // Check if rule is enabled
            if (!rule.getEnabled()) {
                return createSkippedResult(rule, "Rule is disabled");
            }

            // Check context matching
            if (!rule.matchesContext(context)) {
                return createSkippedResult(rule, "Context does not match");
            }

            // Check if already executed (avoid cycles in rule chaining)
            if (context.wasRuleExecuted(rule.getId())) {
                return createSkippedResult(rule, "Rule already executed in this context");
            }

            // Mark as executed
            context.markRuleExecuted(rule.getId());

            // Evaluate temporal window (Phase 2 feature)
            if (temporalExecutor != null && rule.getTemporalWindow() != null) {
                boolean temporalMatch = temporalExecutor.evaluateTemporalRule(rule, context);
                if (!temporalMatch) {
                    RuleResult result = createSkippedResult(rule, "Temporal window condition not met");
                    if (auditLogger != null) {
                        auditLogger.logExecution(rule, context, result);
                    }
                    return result;
                }
            }

            // Evaluate conditions
            boolean matched = evaluateConditions(rule, context);

            RuleResult result = RuleResult.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .contextId(context.getContextId())
                    .matched(matched)
                    .status(matched ? RuleResult.ExecutionStatus.MATCHED : RuleResult.ExecutionStatus.NOT_MATCHED)
                    .executedAt(Instant.now())
                    .build();

            // Execute actions if matched
            if (matched) {
                actionExecutor.executeActions(rule.getActions(), context, result);
            }

            // Store result in context
            context.addRuleResult(rule.getId(), result);

            // Audit logging (Phase 2 feature)
            if (auditLogger != null) {
                auditLogger.logExecution(rule, context, result);
            }

            // WebSocket notification (Phase 4 feature)
            if (executionNotifier != null) {
                executionNotifier.notifyExecution(result);
            }

            logger.debug("Rule {} executed: matched={}, time={}ms",
                    rule.getName(), matched, result.getExecutionTimeMs());

            return result;

        } catch (Exception e) {
            logger.error("Error executing rule: " + rule.getName(), e);
            RuleResult result = RuleResult.error(rule.getId(), rule.getName(), e.getMessage());
            result.setError(e.getMessage(), e);

            // Audit error (Phase 2 feature)
            if (auditLogger != null) {
                auditLogger.logError(rule.getId(), rule.getName(), e.getMessage());
            }

            return result;
        }
    }

    private boolean evaluateConditions(Rule rule, RuleContext context) {
        List<Condition> conditions = rule.getConditions();

        if (conditions.isEmpty()) {
            return true; // No conditions means always match
        }

        Rule.ConditionLogic logic = rule.getConditionLogic();
        if (logic == null) {
            logic = Rule.ConditionLogic.AND; // Default to AND
        }

        return switch (logic) {
            case AND -> conditions.stream().allMatch(c -> evaluateCondition(c, context));
            case OR -> conditions.stream().anyMatch(c -> evaluateCondition(c, context));
            case CUSTOM -> evaluateCustomLogic(conditions, context);
        };
    }

    private boolean evaluateCondition(Condition condition, RuleContext context) {
        // Try each evaluator until one supports the condition
        for (ExpressionEvaluator evaluator : evaluators) {
            try {
                if (condition.isExpression() && evaluator.supports(condition.getExpression())) {
                    return evaluator.evaluate(condition, context);
                } else if (condition.isSimple() || condition.isNested()) {
                    return evaluator.evaluate(condition, context);
                }
            } catch (Exception e) {
                logger.warn("Evaluator {} failed for condition: {}",
                        evaluator.getName(), e.getMessage());
            }
        }

        logger.warn("No evaluator found for condition: {}", condition);
        return false;
    }

    private boolean evaluateCustomLogic(List<Condition> conditions, RuleContext context) {
        // For custom logic, evaluate all conditions and let user define logic
        // This is a placeholder - can be extended with scripting
        return conditions.stream().allMatch(c -> evaluateCondition(c, context));
    }

    @Override
    public List<RuleResult> executeAll(List<Rule> rules, RuleContext context) {
        return rules.stream()
                .map(rule -> execute(rule, context))
                .collect(Collectors.toList());
    }

    @Override
    public RuleResult executeFirst(List<Rule> rules, RuleContext context) {
        for (Rule rule : rules) {
            RuleResult result = execute(rule, context);
            if (result.isMatched()) {
                return result;
            }
        }
        return null;
    }

    @Override
    public List<RuleResult> executePrioritized(List<Rule> rules, RuleContext context) {
        // Sort by priority (descending) and salience
        List<Rule> sortedRules = rules.stream()
                .sorted(Comparator
                        .comparing(Rule::getPriority, Comparator.reverseOrder())
                        .thenComparing(Rule::getSalience, Comparator.reverseOrder()))
                .toList();

        return executeAll(sortedRules, context);
    }

    @Override
    public List<RuleResult> executeWithDependencies(List<Rule> rules, RuleContext context) {
        // Build dependency graph
        Map<String, Rule> ruleMap = rules.stream()
                .collect(Collectors.toMap(Rule::getId, r -> r));

        // Topological sort
        List<Rule> sortedRules = topologicalSort(rules, ruleMap);

        return executeAll(sortedRules, context);
    }

    private List<Rule> topologicalSort(List<Rule> rules, Map<String, Rule> ruleMap) {
        List<Rule> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (Rule rule : rules) {
            if (!visited.contains(rule.getId())) {
                topologicalSortDFS(rule, ruleMap, visited, visiting, sorted);
            }
        }

        return sorted;
    }

    private void topologicalSortDFS(
            Rule rule,
            Map<String, Rule> ruleMap,
            Set<String> visited,
            Set<String> visiting,
            List<Rule> sorted) {

        if (visiting.contains(rule.getId())) {
            throw new IllegalStateException("Circular dependency detected in rules");
        }

        if (visited.contains(rule.getId())) {
            return;
        }

        visiting.add(rule.getId());

        // Visit dependencies first
        for (String depId : rule.getDependsOn()) {
            Rule dep = ruleMap.get(depId);
            if (dep != null) {
                topologicalSortDFS(dep, ruleMap, visited, visiting, sorted);
            }
        }

        visiting.remove(rule.getId());
        visited.add(rule.getId());
        sorted.add(rule);
    }

    private RuleResult createSkippedResult(Rule rule, String reason) {
        return RuleResult.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .matched(false)
                .status(RuleResult.ExecutionStatus.SKIPPED)
                .executedAt(Instant.now())
                .metadata(Map.of("skipReason", reason))
                .build();
    }

    @Override
    public EngineStatistics getStatistics() {
        double avgTime = executionTimes.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        return new EngineStatistics(
                totalExecutions.get(),
                totalMatches.get(),
                totalErrors.get(),
                avgTime,
                0, // TODO: Implement cache stats
                0
        );
    }
}
