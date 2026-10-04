package com.marketflow.engine;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves template variables in node configurations such as {{trigger.email}},
 * {{qualification.score}}, {{step_1.output.name}}, and evaluates Jayway JsonPath expressions.
 */
@Component
public class JsonPathExpressionResolver {

    private static final Logger log = LoggerFactory.getLogger(JsonPathExpressionResolver.class);
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final Pattern EXACT_TEMPLATE_PATTERN = Pattern.compile("^\\s*\\{\\{([^}]+)\\}\\}\\s*$");

    private final Configuration jsonPathConfig;

    public JsonPathExpressionResolver() {
        this.jsonPathConfig = Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS, Option.DEFAULT_PATH_LEAF_TO_NULL)
                .build();
    }

    /**
     * Resolves an arbitrary object (String, Map, List, or primitive) using the ExecutionContext.
     */
    public Object resolve(Object input, ExecutionContext context) {
        if (input == null || context == null) {
            return input;
        }
        Map<String, Object> rootDocument = buildRootDocument(context);
        return resolveWithRoot(input, rootDocument, context);
    }

    /**
     * Resolves a map of key-value pairs recursively.
     */
    public Map<String, Object> resolveMap(Map<String, Object> inputMap, ExecutionContext context) {
        if (inputMap == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> rootDocument = buildRootDocument(context);
        return resolveMapInternal(inputMap, rootDocument, context);
    }

    /**
     * Resolves a string containing {{...}} placeholders or Jayway expressions.
     */
    public String resolveString(String template, ExecutionContext context) {
        if (template == null) {
            return null;
        }
        Map<String, Object> rootDocument = buildRootDocument(context);
        Object resolved = resolveWithRoot(template, rootDocument, context);
        return resolved != null ? resolved.toString() : "";
    }

    /**
     * Evaluates a single expression like "trigger.email", "qualification.score", "$.lead.name".
     */
    public Object evaluateExpression(String expression, ExecutionContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Map<String, Object> rootDocument = buildRootDocument(context);
        return evaluateExpressionInternal(expression.trim(), rootDocument, context);
    }

    public Object evaluateExpression(String expression, Map<String, Object> rootDocument) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        return evaluateExpressionInternal(expression.trim(), rootDocument, null);
    }

    @SuppressWarnings("unchecked")
    private Object resolveWithRoot(Object input, Map<String, Object> rootDocument, ExecutionContext context) {
        if (input instanceof String str) {
            return resolveStringTemplate(str, rootDocument, context);
        } else if (input instanceof Map<?, ?> map) {
            return resolveMapInternal((Map<String, Object>) map, rootDocument, context);
        } else if (input instanceof List<?> list) {
            List<Object> resolvedList = new ArrayList<>(list.size());
            for (Object item : list) {
                resolvedList.add(resolveWithRoot(item, rootDocument, context));
            }
            return resolvedList;
        }
        return input;
    }

    private Map<String, Object> resolveMapInternal(Map<String, Object> inputMap,
                                                   Map<String, Object> rootDocument,
                                                   ExecutionContext context) {
        Map<String, Object> resolvedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
            String resolvedKey = entry.getKey();
            if (resolvedKey != null && resolvedKey.contains("{{")) {
                Object k = resolveStringTemplate(resolvedKey, rootDocument, context);
                resolvedKey = k != null ? k.toString() : resolvedKey;
            }
            Object resolvedValue = resolveWithRoot(entry.getValue(), rootDocument, context);
            resolvedMap.put(resolvedKey, resolvedValue);
        }
        return resolvedMap;
    }

    private Object resolveStringTemplate(String str, Map<String, Object> rootDocument, ExecutionContext context) {
        if (str == null || str.isBlank()) {
            return str;
        }

        // Check if string is EXACTLY a single token, e.g. "{{trigger.score}}"
        Matcher exactMatcher = EXACT_TEMPLATE_PATTERN.matcher(str);
        if (exactMatcher.matches()) {
            String expr = exactMatcher.group(1).trim();
            return evaluateExpressionInternal(expr, rootDocument, context);
        }

        // If string contains one or more {{...}} embedded within other text
        Matcher matcher = TEMPLATE_PATTERN.matcher(str);
        if (!matcher.find()) {
            // Check if string is a raw JsonPath expression like "$.trigger.email"
            if (str.startsWith("$.") || str.startsWith("$[")) {
                return evaluateJsonPath(str, rootDocument);
            }
            return str;
        }

        matcher.reset();
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String expr = matcher.group(1).trim();
            Object value = evaluateExpressionInternal(expr, rootDocument, context);
            String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Object evaluateExpressionInternal(String expr, Map<String, Object> rootDocument, ExecutionContext context) {
        if (expr == null || expr.isBlank()) {
            return null;
        }

        // Strip enclosing {{ }} if still present
        if (expr.startsWith("{{") && expr.endsWith("}}")) {
            expr = expr.substring(2, expr.length() - 2).trim();
        }

        // Direct JsonPath expression starting with $
        if (expr.startsWith("$")) {
            return evaluateJsonPath(expr, rootDocument);
        }

        // Try Jayway with $. prefix
        String jsonPath = "$." + expr;
        Object val = evaluateJsonPath(jsonPath, rootDocument);
        if (val != null) {
            return val;
        }

        // Try direct root map lookup
        if (rootDocument.containsKey(expr)) {
            return rootDocument.get(expr);
        }

        // Try manual dot-path navigation (handles keys with special chars or nested maps)
        Object dotVal = resolveDotPath(rootDocument, expr);
        if (dotVal != null) {
            return dotVal;
        }

        // Fallback to ExecutionContext getVariable if available
        if (context != null) {
            Object varVal = context.getVariable(expr);
            if (varVal != null) {
                return varVal;
            }
        }

        return null;
    }

    private Object evaluateJsonPath(String jsonPath, Map<String, Object> rootDocument) {
        try {
            return JsonPath.using(jsonPathConfig).parse(rootDocument).read(jsonPath);
        } catch (Exception e) {
            log.trace("JsonPath evaluation failed for [{}]: {}", jsonPath, e.getMessage());
            return null;
        }
    }

    private Object resolveDotPath(Map<String, Object> root, String path) {
        if (path == null || root == null) return null;
        String[] parts = path.split("\\.");
        Object current = root;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    public Map<String, Object> buildRootDocument(ExecutionContext context) {
        Map<String, Object> root = new LinkedHashMap<>();

        // 1. Initial trigger payload under "trigger"
        Map<String, Object> triggerPayload = context.getTriggerPayload();
        if (triggerPayload != null) {
            root.put("trigger", triggerPayload);
            // Also spread trigger payload to root level for convenient access
            root.putAll(triggerPayload);
        }

        // 2. Global variables
        Map<String, Object> variables = context.getVariables();
        if (variables != null) {
            root.putAll(variables);
        }

        // 3. Accumulated node outputs
        Map<String, Object> nodeOutputs = context.getNodeOutputs();
        if (nodeOutputs != null) {
            for (Map.Entry<String, Object> entry : nodeOutputs.entrySet()) {
                root.put(entry.getKey(), entry.getValue());
                // If output contains nested map, wrap with "output" namespace as well: {{nodeId.output.property}}
                if (entry.getValue() instanceof Map<?, ?> outMap) {
                    Map<String, Object> alias = new LinkedHashMap<>();
                    alias.put("output", outMap);
                    for (Map.Entry<?, ?> e : outMap.entrySet()) {
                        if (e.getKey() != null) {
                            alias.put(e.getKey().toString(), e.getValue());
                        }
                    }
                    root.put(entry.getKey(), alias);
                }
            }
        }

        return root;
    }
}
