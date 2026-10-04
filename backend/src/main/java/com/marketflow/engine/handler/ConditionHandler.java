package com.marketflow.engine.handler;

import com.marketflow.dto.NodeDto;
import com.marketflow.engine.ExecutionContext;
import com.marketflow.engine.JsonPathExpressionResolver;
import com.marketflow.engine.NodeExecutionResult;
import com.marketflow.engine.NodeExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Phase 5: ConditionHandler
 * Evaluates boolean conditions with rich operators (>, <, >=, <=, ==, !=, CONTAINS, NOT_CONTAINS, REGEX,
 * STARTS_WITH, ENDS_WITH, IS_EMPTY, IS_NOT_EMPTY, IN), supporting single rules, dynamic template expressions,
 * and compound AND/OR rule groups.
 */
@Component
public class ConditionHandler implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ConditionHandler.class);
    private final JsonPathExpressionResolver expressionResolver;

    public ConditionHandler(JsonPathExpressionResolver expressionResolver) {
        this.expressionResolver = expressionResolver;
    }

    @Override
    public boolean supports(String nodeType) {
        if (nodeType == null) return false;
        String type = nodeType.toLowerCase();
        return type.equals("condition") || type.startsWith("condition_")
                || type.contains("condition") || type.contains("filter")
                || type.equals("branch") || type.equals("if_else");
    }

    @Override
    public NodeExecutionResult execute(NodeDto node, ExecutionContext context) {
        Map<String, Object> data = node.getData() != null ? node.getData() : Collections.emptyMap();

        boolean finalResult;
        String mainField = "score";
        String mainOperator = ">";
        Object mainActualVal = null;
        Object mainExpectedVal = null;

        // Check if node data defines compound rules
        if (data.containsKey("rules") && data.get("rules") instanceof List<?> ruleList && !ruleList.isEmpty()) {
            String logicalOp = data.getOrDefault("logicalOperator", "AND").toString().toUpperCase();
            boolean isAnd = !logicalOp.equals("OR");
            finalResult = isAnd;

            List<Map<String, Object>> ruleEvaluations = new ArrayList<>();

            for (Object ruleObj : ruleList) {
                if (ruleObj instanceof Map<?, ?> ruleMap) {
                    Object fieldObj = ruleMap.get("field");
                    String field = fieldObj != null ? fieldObj.toString() : "score";
                    Object opObj = ruleMap.get("operator");
                    String operator = opObj != null ? opObj.toString() : "==";
                    Object expected = ruleMap.get("value");

                    Object actual = resolveFieldValue(field, context);
                    Object resolvedExpected = expressionResolver.resolve(expected, context);

                    boolean ruleResult = evaluateCondition(actual, operator, resolvedExpected);

                    Map<String, Object> eval = new LinkedHashMap<>();
                    eval.put("field", field);
                    eval.put("operator", operator);
                    eval.put("actualValue", actual);
                    eval.put("expectedValue", resolvedExpected);
                    eval.put("result", ruleResult);
                    ruleEvaluations.add(eval);

                    if (isAnd) {
                        finalResult = finalResult && ruleResult;
                    } else {
                        finalResult = finalResult || ruleResult;
                    }
                }
            }

            mainField = "rules (" + ruleList.size() + ")";
            mainOperator = logicalOp;
            mainActualVal = ruleEvaluations;
            mainExpectedVal = finalResult;

        } else {
            // Single rule condition
            mainField = data.containsKey("field") ? data.get("field").toString() : "score";
            mainOperator = data.containsKey("operator") ? data.get("operator").toString() : ">";
            Object rawThreshold = data.getOrDefault("value", 70);

            mainActualVal = resolveFieldValue(mainField, context);
            mainExpectedVal = expressionResolver.resolve(rawThreshold, context);

            finalResult = evaluateCondition(mainActualVal, mainOperator, mainExpectedVal);
        }

        String branch = finalResult ? "true" : "false";

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("field", mainField);
        output.put("actualValue", mainActualVal != null ? mainActualVal : "");
        output.put("operator", mainOperator);
        output.put("expectedValue", mainExpectedVal != null ? mainExpectedVal : "");
        output.put("result", finalResult);
        output.put("branch", branch);

        log.info("Condition node [{}] evaluated: field=[{}] {} [{}] -> branch=[{}]",
                node.getId(), mainField, mainOperator, mainExpectedVal, branch);

        return NodeExecutionResult.conditional(node.getId(), output, branch);
    }

    private Object resolveFieldValue(String field, ExecutionContext context) {
        if (field == null || field.isBlank()) {
            return null;
        }

        // If field contains {{ ... }} template syntax
        if (field.contains("{{")) {
            return expressionResolver.resolve(field, context);
        }

        // Try Jayway / Expression resolution first
        Object val = expressionResolver.evaluateExpression(field, context);
        if (val != null) {
            return val;
        }

        // Try direct variable / trigger lookup
        return context.getVariable(field);
    }

    public boolean evaluateCondition(Object actual, String operator, Object expected) {
        if (operator == null) return false;
        String op = operator.trim().toUpperCase();

        // Null / Empty checks
        switch (op) {
            case "IS_NULL" -> { return actual == null; }
            case "IS_NOT_NULL" -> { return actual != null; }
            case "IS_EMPTY" -> {
                return actual == null || actual.toString().trim().isEmpty();
            }
            case "IS_NOT_EMPTY" -> {
                return actual != null && !actual.toString().trim().isEmpty();
            }
        }

        if (actual == null) {
            return false;
        }

        // REGEX Pattern match
        if (op.equals("REGEX") || op.equals("MATCHES")) {
            if (expected == null) return false;
            try {
                Pattern pattern = Pattern.compile(expected.toString());
                return pattern.matcher(actual.toString()).find();
            } catch (Exception e) {
                log.warn("Invalid regex pattern [{}]: {}", expected, e.getMessage());
                return false;
            }
        }

        // IN / NOT_IN collection checks
        if (op.equals("IN") || op.equals("NOT_IN")) {
            boolean in = checkIn(actual, expected);
            return op.equals("IN") ? in : !in;
        }

        // Numeric comparison
        if (isNumeric(actual) && isNumeric(expected)) {
            double dActual = Double.parseDouble(actual.toString().trim());
            double dExpected = Double.parseDouble(expected.toString().trim());
            return switch (operator) {
                case ">" -> dActual > dExpected;
                case ">=" -> dActual >= dExpected;
                case "<" -> dActual < dExpected;
                case "<=" -> dActual <= dExpected;
                case "==" -> Math.abs(dActual - dExpected) < 0.00001;
                case "!=" -> Math.abs(dActual - dExpected) >= 0.00001;
                default -> false;
            };
        }

        // Boolean comparison
        if (isBoolean(actual) && isBoolean(expected)) {
            boolean bActual = Boolean.parseBoolean(actual.toString().trim());
            boolean bExpected = Boolean.parseBoolean(expected.toString().trim());
            return switch (operator) {
                case "==", "EQUALS" -> bActual == bExpected;
                case "!=", "NOT_EQUALS" -> bActual != bExpected;
                default -> false;
            };
        }

        // String comparison
        String sActual = actual.toString();
        String sExpected = expected != null ? expected.toString() : "";

        return switch (op) {
            case "CONTAINS" -> sActual.toLowerCase().contains(sExpected.toLowerCase());
            case "NOT_CONTAINS" -> !sActual.toLowerCase().contains(sExpected.toLowerCase());
            case "STARTS_WITH" -> sActual.toLowerCase().startsWith(sExpected.toLowerCase());
            case "ENDS_WITH" -> sActual.toLowerCase().endsWith(sExpected.toLowerCase());
            case "==", "EQUALS" -> sActual.equalsIgnoreCase(sExpected);
            case "!=", "NOT_EQUALS" -> !sActual.equalsIgnoreCase(sExpected);
            default -> false;
        };
    }

    private boolean checkIn(Object actual, Object expected) {
        if (expected == null) return false;
        String actStr = actual.toString().trim();

        if (expected instanceof Collection<?> coll) {
            return coll.stream().anyMatch(item -> item != null && item.toString().trim().equalsIgnoreCase(actStr));
        }

        if (expected.getClass().isArray()) {
            Object[] arr = (Object[]) expected;
            return Arrays.stream(arr).anyMatch(item -> item != null && item.toString().trim().equalsIgnoreCase(actStr));
        }

        // Comma-separated list string, e.g. "US, CA, UK"
        String[] tokens = expected.toString().split(",");
        return Arrays.stream(tokens)
                .map(String::trim)
                .anyMatch(tok -> tok.equalsIgnoreCase(actStr));
    }

    private boolean isNumeric(Object obj) {
        if (obj == null) return false;
        try {
            Double.parseDouble(obj.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBoolean(Object obj) {
        if (obj == null) return false;
        String s = obj.toString().trim().toLowerCase();
        return s.equals("true") || s.equals("false");
    }
}
