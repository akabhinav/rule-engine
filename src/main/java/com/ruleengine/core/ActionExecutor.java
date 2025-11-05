package com.ruleengine.core;

import com.ruleengine.model.Action;
import com.ruleengine.model.RuleContext;
import com.ruleengine.model.RuleResult;
import com.ruleengine.streaming.KafkaRuleProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Action Executor - Executes rule actions (Phase 2 Enhanced)
 *
 * Supports:
 * - Setting fact values
 * - Triggering other rules
 * - Executing scripts (sandboxed)
 * - Calling external services
 * - Emitting events to Kafka
 */
@Component
public class ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ActionExecutor.class);

    private final ScriptExecutor scriptExecutor;
    private final KafkaRuleProducer kafkaProducer;

    public ActionExecutor(
            @Autowired(required = false) ScriptExecutor scriptExecutor,
            @Autowired(required = false) KafkaRuleProducer kafkaProducer) {
        this.scriptExecutor = scriptExecutor;
        this.kafkaProducer = kafkaProducer;
    }

    public void executeActions(List<Action> actions, RuleContext context, RuleResult result) {
        for (Action action : actions) {
            try {
                executeAction(action, context, result);
            } catch (Exception e) {
                logger.error("Error executing action: " + action.getType(), e);
                result.addActionResult(action.getId(), false, null);
            }
        }
    }

    private void executeAction(Action action, RuleContext context, RuleResult result) {
        switch (action.getType()) {
            case SET_FACT -> executeSetFact(action, context, result);
            case TRIGGER_RULE -> executeTriggerRule(action, context, result);
            case EXECUTE_SCRIPT -> executeScript(action, context, result);
            case CALL_SERVICE -> executeCallService(action, context, result);
            case EMIT_EVENT -> executeEmitEvent(action, context, result);
            case ENRICH_RESULT -> executeEnrichResult(action, context, result);
            case LOG -> executeLog(action, context, result);
            default -> logger.warn("Unknown action type: {}", action.getType());
        }
    }

    private void executeSetFact(Action action, RuleContext context, RuleResult result) {
        String factKey = action.getFactKey();
        Object value = action.getFactValue();

        // Handle dynamic value expressions
        if (action.getValueExpression() != null) {
            // TODO: Evaluate expression to get value
            value = evaluateValueExpression(action.getValueExpression(), context);
        }

        // Set value in context
        context.setIntermediateResult(factKey, value);

        result.addActionResult(action.getId(), true, value);
        logger.debug("Set fact: {} = {}", factKey, value);
    }

    private void executeTriggerRule(Action action, RuleContext context, RuleResult result) {
        String ruleId = action.getRuleId();

        // Store trigger request
        context.setIntermediateResult("trigger:" + ruleId, true);

        result.addActionResult(action.getId(), true, ruleId);
        logger.debug("Triggered rule: {}", ruleId);
    }

    private void executeScript(Action action, RuleContext context, RuleResult result) {
        // Phase 2: Sandboxed script execution
        if (scriptExecutor == null) {
            logger.warn("ScriptExecutor not available");
            result.addActionResult(action.getId(), false, "ScriptExecutor not configured");
            return;
        }

        try {
            Object scriptResult = scriptExecutor.executeScript(action, context);
            result.addActionResult(action.getId(), true, scriptResult);
            logger.debug("Script executed successfully");
        } catch (Exception e) {
            logger.error("Script execution failed", e);
            result.addActionResult(action.getId(), false, e.getMessage());
        }
    }

    private void executeCallService(Action action, RuleContext context, RuleResult result) {
        // TODO: Implement external service calls (future enhancement)
        logger.warn("Service call not yet implemented");
        result.addActionResult(action.getId(), false, "Not implemented");
    }

    private void executeEmitEvent(Action action, RuleContext context, RuleResult result) {
        // Phase 2: Kafka event emission
        if (kafkaProducer == null) {
            logger.warn("KafkaProducer not available");
            result.addActionResult(action.getId(), false, "Kafka not configured");
            return;
        }

        try {
            String topic = action.getEventTopic();
            Object payload = action.getEventPayload();

            kafkaProducer.sendEvent(topic, context.getContextId(), payload);
            result.addActionResult(action.getId(), true, "Event emitted to " + topic);
            logger.debug("Event emitted to Kafka topic: {}", topic);
        } catch (Exception e) {
            logger.error("Event emission failed", e);
            result.addActionResult(action.getId(), false, e.getMessage());
        }
    }

    private void executeEnrichResult(Action action, RuleContext context, RuleResult result) {
        // Add enrichment data to result
        if (action.getMetadata() != null) {
            action.getMetadata().forEach(result::addEnrichment);
        }

        result.addActionResult(action.getId(), true, "Enriched");
        logger.debug("Enriched result with: {}", action.getMetadata());
    }

    private void executeLog(Action action, RuleContext context, RuleResult result) {
        logger.info("Rule action log: {}", action.getDescription());
        result.addActionResult(action.getId(), true, "Logged");
    }

    private Object evaluateValueExpression(String expression, RuleContext context) {
        // TODO: Use expression evaluator to compute dynamic values
        return expression;
    }
}
