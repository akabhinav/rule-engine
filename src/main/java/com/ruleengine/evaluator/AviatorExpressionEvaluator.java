package com.ruleengine.evaluator;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.ruleengine.model.Condition;
import com.ruleengine.model.RuleContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Aviator Expression Evaluator
 *
 * High-performance expression evaluator with compilation support
 * Supports complex expressions like:
 * - "transaction.amount > 1000 && user.riskScore < 50"
 * - "price * quantity > 5000"
 * - "string.contains(user.email, '@gmail.com')"
 */
@Component
public class AviatorExpressionEvaluator implements ExpressionEvaluator {

    // Cache for compiled expressions
    private final Map<String, Expression> compiledCache = new HashMap<>();

    public AviatorExpressionEvaluator() {
        // Configure Aviator
        AviatorEvaluator.getInstance().setOption(
            com.googlecode.aviator.Options.ALWAYS_PARSE_FLOATING_POINT_NUMBER_INTO_DECIMAL,
            true
        );
    }

    @Override
    public boolean evaluate(Condition condition, RuleContext context) {
        if (!condition.isExpression()) {
            return false;
        }

        String expression = condition.getExpression();
        Map<String, Object> variables = buildVariables(context);

        try {
            Object result = evaluateExpression(expression, variables);
            return result instanceof Boolean ? (Boolean) result : false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate expression: " + expression, e);
        }
    }

    @Override
    public Object evaluateExpression(String expression, Map<String, Object> variables) {
        try {
            return AviatorEvaluator.execute(expression, variables);
        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate expression: " + expression, e);
        }
    }

    @Override
    public Object compileExpression(String expression) {
        return compiledCache.computeIfAbsent(expression, expr -> {
            try {
                return AviatorEvaluator.compile(expr);
            } catch (Exception e) {
                throw new RuntimeException("Failed to compile expression: " + expr, e);
            }
        });
    }

    @Override
    public Object evaluateCompiled(Object compiled, Map<String, Object> variables) {
        if (!(compiled instanceof Expression)) {
            throw new IllegalArgumentException("Invalid compiled expression");
        }

        try {
            return ((Expression) compiled).execute(variables);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute compiled expression", e);
        }
    }

    @Override
    public String getName() {
        return "AviatorEvaluator";
    }

    @Override
    public boolean supports(String expression) {
        // Aviator supports most expressions
        return expression != null && !expression.isEmpty();
    }

    /**
     * Build variables map from context
     */
    private Map<String, Object> buildVariables(RuleContext context) {
        Map<String, Object> variables = new HashMap<>();

        // Add all facts as variables
        context.getFacts().forEach((key, fact) -> {
            variables.put(key, fact.getData());
        });

        // Add context attributes
        variables.putAll(context.getContextAttributes());

        // Add intermediate results
        variables.putAll(context.getIntermediateResults());

        return variables;
    }

    /**
     * Clear compiled expression cache
     */
    public void clearCache() {
        compiledCache.clear();
    }

    /**
     * Get cache size
     */
    public int getCacheSize() {
        return compiledCache.size();
    }
}
