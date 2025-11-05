package com.ruleengine.evaluator;

import com.ruleengine.model.Condition;
import com.ruleengine.model.RuleContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Simple Expression Evaluator
 *
 * Lightweight evaluator for simple field-operator-value conditions
 * Zero compilation overhead, optimized for speed
 */
@Component
public class SimpleExpressionEvaluator implements ExpressionEvaluator {

    private final OperatorRegistry operatorRegistry;

    public SimpleExpressionEvaluator(OperatorRegistry operatorRegistry) {
        this.operatorRegistry = operatorRegistry;
    }

    @Override
    public boolean evaluate(Condition condition, RuleContext context) {
        if (condition.isNested()) {
            return evaluateNested(condition, context);
        }

        if (condition.isSimple()) {
            return evaluateSimple(condition, context);
        }

        if (condition.isExpression()) {
            // For complex expressions, delegate to another evaluator
            return false;
        }

        return false;
    }

    private boolean evaluateSimple(Condition condition, RuleContext context) {
        String field = condition.getField();
        Condition.Operator operator = condition.getOperator();
        Object expectedValue = condition.getValue();

        // Get actual value from context
        Object actualValue = context.getFactValue(field);

        return compareValues(actualValue, operator, expectedValue);
    }

    private boolean evaluateNested(Condition condition, RuleContext context) {
        List<Condition> nested = condition.getNestedConditions();
        Condition.LogicalOperator logicalOp = condition.getLogicalOperator();

        if (nested.isEmpty()) {
            return true;
        }

        if (logicalOp == Condition.LogicalOperator.AND) {
            return nested.stream().allMatch(c -> evaluate(c, context));
        } else if (logicalOp == Condition.LogicalOperator.OR) {
            return nested.stream().anyMatch(c -> evaluate(c, context));
        } else if (logicalOp == Condition.LogicalOperator.NOT) {
            return !evaluate(nested.get(0), context);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean compareValues(Object actual, Condition.Operator operator, Object expected) {
        // Null checks
        if (operator == Condition.Operator.IS_NULL) {
            return actual == null;
        }
        if (operator == Condition.Operator.IS_NOT_NULL) {
            return actual != null;
        }

        // If actual is null and we're not checking for null, return false
        if (actual == null) {
            return false;
        }

        switch (operator) {
            case EQUALS:
                return Objects.equals(actual, expected);

            case NOT_EQUALS:
                return !Objects.equals(actual, expected);

            case GREATER_THAN:
                return compareNumeric(actual, expected) > 0;

            case GREATER_THAN_OR_EQUALS:
                return compareNumeric(actual, expected) >= 0;

            case LESS_THAN:
                return compareNumeric(actual, expected) < 0;

            case LESS_THAN_OR_EQUALS:
                return compareNumeric(actual, expected) <= 0;

            case CONTAINS:
                return actual.toString().contains(expected.toString());

            case STARTS_WITH:
                return actual.toString().startsWith(expected.toString());

            case ENDS_WITH:
                return actual.toString().endsWith(expected.toString());

            case MATCHES:
                return Pattern.matches(expected.toString(), actual.toString());

            case IN:
                if (expected instanceof Collection) {
                    return ((Collection<?>) expected).contains(actual);
                }
                return false;

            case NOT_IN:
                if (expected instanceof Collection) {
                    return !((Collection<?>) expected).contains(actual);
                }
                return true;

            case BETWEEN:
                if (expected instanceof List && ((List<?>) expected).size() == 2) {
                    List<?> range = (List<?>) expected;
                    int compareMin = compareNumeric(actual, range.get(0));
                    int compareMax = compareNumeric(actual, range.get(1));
                    return compareMin >= 0 && compareMax <= 0;
                }
                return false;

            case CUSTOM:
                // Delegate to operator registry
                return operatorRegistry.evaluate(operator.name(), actual, expected);

            default:
                return false;
        }
    }

    private int compareNumeric(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            double a = ((Number) actual).doubleValue();
            double e = ((Number) expected).doubleValue();
            return Double.compare(a, e);
        }

        // Try string comparison
        return actual.toString().compareTo(expected.toString());
    }

    @Override
    public Object evaluateExpression(String expression, Map<String, Object> variables) {
        // Simple evaluator doesn't support complex expressions
        throw new UnsupportedOperationException("Use AviatorExpressionEvaluator for complex expressions");
    }

    @Override
    public Object compileExpression(String expression) {
        return expression; // No compilation needed
    }

    @Override
    public Object evaluateCompiled(Object compiled, Map<String, Object> variables) {
        return evaluateExpression((String) compiled, variables);
    }

    @Override
    public String getName() {
        return "SimpleEvaluator";
    }

    @Override
    public boolean supports(String expression) {
        // Only supports simple field-operator-value patterns
        return false; // Let other evaluators handle string expressions
    }
}
