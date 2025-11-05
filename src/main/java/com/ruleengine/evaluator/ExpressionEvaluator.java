package com.ruleengine.evaluator;

import com.ruleengine.model.Condition;
import com.ruleengine.model.RuleContext;

import java.util.Map;

/**
 * Expression Evaluator Interface
 *
 * Implementations:
 * - AviatorExpressionEvaluator (Aviator)
 * - MvelExpressionEvaluator (MVEL)
 * - SimpleExpressionEvaluator (Custom lightweight)
 */
public interface ExpressionEvaluator {

    /**
     * Evaluate a condition against a context
     */
    boolean evaluate(Condition condition, RuleContext context);

    /**
     * Evaluate a raw expression string
     */
    Object evaluateExpression(String expression, Map<String, Object> variables);

    /**
     * Compile expression for reuse (optimization)
     */
    Object compileExpression(String expression);

    /**
     * Evaluate compiled expression
     */
    Object evaluateCompiled(Object compiled, Map<String, Object> variables);

    /**
     * Get evaluator name
     */
    String getName();

    /**
     * Check if this evaluator supports the given expression
     */
    boolean supports(String expression);
}
