package com.marketflow.engine.handler;

import com.marketflow.dto.NodeDto;
import com.marketflow.engine.ExecutionContext;
import com.marketflow.engine.NodeExecutionResult;
import com.marketflow.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ConditionNodeExecutor implements NodeExecutor {

    @Override
    public boolean supports(String nodeType) {
        if (nodeType == null) return false;
        String type = nodeType.toLowerCase();
        return type.equals("condition") || type.startsWith("condition_") || type.contains("condition") || type.contains("filter");
    }

    @Override
    public NodeExecutionResult execute(NodeDto node, ExecutionContext context) {
        Map<String, Object> data = node.getData() != null ? node.getData() : new HashMap<>();
        String field = data.containsKey("field") ? data.get("field").toString() : "score";
        String operator = data.containsKey("operator") ? data.get("operator").toString() : ">";
        Object thresholdObj = data.getOrDefault("value", 70);

        // Resolve field value from trigger payload or context variables
        Object actualVal = resolveValue(field, context);
        boolean result = evaluateCondition(actualVal, operator, thresholdObj);

        String branch = result ? "true" : "false";

        Map<String, Object> output = new HashMap<>();
        output.put("field", field);
        output.put("actualValue", actualVal);
        output.put("operator", operator);
        output.put("expectedValue", thresholdObj);
        output.put("result", result);
        output.put("branch", branch);

        return NodeExecutionResult.conditional(node.getId(), output, branch);
    }

    private Object resolveValue(String field, ExecutionContext context) {
        Object direct = context.getVariable(field);
        if (direct != null) return direct;

        Map<String, Object> trigger = context.getTriggerPayload();
        if (trigger.containsKey(field)) {
            return trigger.get(field);
        }
        return null;
    }

    private boolean evaluateCondition(Object actual, String operator, Object expected) {
        if (actual == null) return false;

        // Numeric comparison
        if (isNumeric(actual) && isNumeric(expected)) {
            double dActual = Double.parseDouble(actual.toString());
            double dExpected = Double.parseDouble(expected.toString());
            return switch (operator) {
                case ">" -> dActual > dExpected;
                case ">=" -> dActual >= dExpected;
                case "<" -> dActual < dExpected;
                case "<=" -> dActual <= dExpected;
                case "==" -> Math.abs(dActual - dExpected) < 0.0001;
                case "!=" -> Math.abs(dActual - dExpected) >= 0.0001;
                default -> false;
            };
        }

        // String comparison
        String sActual = actual.toString();
        String sExpected = expected != null ? expected.toString() : "";
        return switch (operator.toUpperCase()) {
            case "CONTAINS" -> sActual.toLowerCase().contains(sExpected.toLowerCase());
            case "NOT_CONTAINS" -> !sActual.toLowerCase().contains(sExpected.toLowerCase());
            case "==" -> sActual.equalsIgnoreCase(sExpected);
            case "!=" -> !sActual.equalsIgnoreCase(sExpected);
            default -> false;
        };
    }

    private boolean isNumeric(Object obj) {
        if (obj == null) return false;
        try {
            Double.parseDouble(obj.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
