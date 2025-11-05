package com.ruleengine.core;

import com.ruleengine.model.Action;
import com.ruleengine.model.RuleContext;
import com.ruleengine.model.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Action Executor - Executes rule actions
 *
 * Supports:
 * - Setting fact values
 * - Triggering other rules
 * - Executing scripts
 * - Calling external services
 * - Emitting events
 */
@Component
public class ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ActionExecutor.class);

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
        // TODO: Implement sandboxed script execution
        logger.warn("Script execution not yet implemented");
        result.addActionResult(action.getId(), false, "Not implemented");
    }

    private void executeCallService(Action action, RuleContext context, RuleResult result) {
        // TODO: Implement external service calls
        logger.warn("Service call not yet implemented");
        result.addActionResult(action.getId(), false, "Not implemented");
    }

    private void executeEmitEvent(Action action, RuleContext context, RuleResult result) {
        // TODO: Implement event emission to Kafka
        logger.warn("Event emission not yet implemented");
        result.addActionResult(action.getId(), false, "Not implemented");
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
