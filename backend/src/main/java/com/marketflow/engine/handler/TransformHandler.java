package com.marketflow.engine.handler;

import com.marketflow.dto.NodeDto;
import com.marketflow.engine.ExecutionContext;
import com.marketflow.engine.JsonPathExpressionResolver;
import com.marketflow.engine.NodeExecutionResult;
import com.marketflow.engine.NodeExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Phase 5: TransformHandler
 * Supports dynamic schema mapping, field extraction, default fallback values,
 * and data transformations (uppercase, lowercase, trim, type conversion).
 */
@Component
public class TransformHandler implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(TransformHandler.class);
    private final JsonPathExpressionResolver expressionResolver;

    public TransformHandler(JsonPathExpressionResolver expressionResolver) {
        this.expressionResolver = expressionResolver;
    }

    @Override
    public boolean supports(String nodeType) {
        if (nodeType == null) return false;
        String type = nodeType.toLowerCase();
        return type.equals("transform") || type.startsWith("transform_")
                || type.contains("transform") || type.contains("mapping")
                || type.equals("data_transformation") || type.equals("extract");
    }

    @Override
    public NodeExecutionResult execute(NodeDto node, ExecutionContext context) {
        Map<String, Object> data = node.getData() != null ? node.getData() : Collections.emptyMap();
        Map<String, Object> transformedResult = new LinkedHashMap<>();

        // 1. Handle Extract if specified
        if (data.containsKey("extract") && data.get("extract") != null) {
            String extractPath = data.get("extract").toString();
            Object extracted = expressionResolver.evaluateExpression(extractPath, context);
            if (extracted instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null) {
                        transformedResult.put(entry.getKey().toString(), entry.getValue());
                    }
                }
            } else if (extracted != null) {
                String targetKey = data.containsKey("target") ? data.get("target").toString() : "extracted";
                transformedResult.put(targetKey, extracted);
            }
        }

        // 2. Handle Schema Mappings (Map<String, Object>)
        if (data.containsKey("mappings") && data.get("mappings") instanceof Map<?, ?> mappings) {
            for (Map.Entry<?, ?> entry : mappings.entrySet()) {
                String targetField = entry.getKey().toString();
                Object rawExpr = entry.getValue();
                Object resolved = expressionResolver.resolve(rawExpr, context);
                transformedResult.put(targetField, resolved);
            }
        }

        // 3. Handle Field Definitions (List of Field Mapping objects)
        if (data.containsKey("fields") && data.get("fields") instanceof List<?> fields) {
            for (Object item : fields) {
                if (item instanceof Map<?, ?> fieldMap) {
                    processFieldMapping(fieldMap, transformedResult, context);
                }
            }
        }

        // 4. Handle Specific Operations (List of Operations)
        if (data.containsKey("operations") && data.get("operations") instanceof List<?> ops) {
            for (Object opObj : ops) {
                if (opObj instanceof Map<?, ?> opMap) {
                    processOperation(opMap, transformedResult, context);
                }
            }
        }

        // 5. Fallback: If neither mappings nor fields specified, resolve arbitrary custom data properties
        if (!data.containsKey("mappings") && !data.containsKey("fields") && !data.containsKey("extract")) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                if (isSystemProperty(key)) continue;
                Object resolved = expressionResolver.resolve(entry.getValue(), context);
                transformedResult.put(key, resolved);
            }
        }

        // Apply Default Fallbacks if specified in "defaults" map
        if (data.containsKey("defaults") && data.get("defaults") instanceof Map<?, ?> defaults) {
            for (Map.Entry<?, ?> entry : defaults.entrySet()) {
                String field = entry.getKey().toString();
                Object currentVal = transformedResult.get(field);
                if (currentVal == null || currentVal.toString().isBlank()) {
                    transformedResult.put(field, entry.getValue());
                }
            }
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("nodeId", node.getId());
        output.put("nodeLabel", node.getLabel());
        output.put("transformedAt", Instant.now().toString());
        output.put("transformed", transformedResult);
        // Expose transformed fields at top level for easy downstream referencing
        output.putAll(transformedResult);

        log.info("Transform node [{}] executed successfully with {} fields", node.getId(), transformedResult.size());
        return NodeExecutionResult.success(node.getId(), output);
    }

    private void processFieldMapping(Map<?, ?> fieldMap, Map<String, Object> targetMap, ExecutionContext context) {
        String target = fieldMap.containsKey("target") ? fieldMap.get("target").toString() : null;
        if (target == null || target.isBlank()) return;

        Object source = fieldMap.get("source");
        Object resolved = source != null ? expressionResolver.resolve(source, context) : null;

        // Fallback value
        if ((resolved == null || (resolved instanceof String s && s.isBlank())) && fieldMap.containsKey("defaultValue")) {
            resolved = fieldMap.get("defaultValue");
        }

        // Transform operation if defined on the field
        if (fieldMap.containsKey("transform") && resolved != null) {
            String transformType = fieldMap.get("transform").toString();
            resolved = applyStringTransform(resolved, transformType);
        }

        // Type conversion
        if (fieldMap.containsKey("type") && resolved != null) {
            String targetType = fieldMap.get("type").toString();
            resolved = convertType(resolved, targetType);
        }

        targetMap.put(target, resolved);
    }

    private void processOperation(Map<?, ?> opMap, Map<String, Object> targetMap, ExecutionContext context) {
        String opType = opMap.containsKey("operation") ? opMap.get("operation").toString().toUpperCase() : "";
        String targetField = opMap.containsKey("target") ? opMap.get("target").toString() : null;
        if (targetField == null) return;

        Object sourceVal = opMap.containsKey("source") 
                ? expressionResolver.resolve(opMap.get("source"), context) 
                : targetMap.get(targetField);

        switch (opType) {
            case "UPPERCASE" -> {
                if (sourceVal != null) targetMap.put(targetField, sourceVal.toString().toUpperCase());
            }
            case "LOWERCASE" -> {
                if (sourceVal != null) targetMap.put(targetField, sourceVal.toString().toLowerCase());
            }
            case "TRIM" -> {
                if (sourceVal != null) targetMap.put(targetField, sourceVal.toString().trim());
            }
            case "TO_NUMBER" -> {
                if (sourceVal != null) targetMap.put(targetField, parseNumber(sourceVal));
            }
            case "TO_BOOLEAN" -> {
                if (sourceVal != null) targetMap.put(targetField, Boolean.parseBoolean(sourceVal.toString()));
            }
            case "REPLACE" -> {
                if (sourceVal != null && opMap.containsKey("find") && opMap.containsKey("replacement")) {
                    String find = opMap.get("find").toString();
                    String replacement = opMap.get("replacement").toString();
                    targetMap.put(targetField, sourceVal.toString().replace(find, replacement));
                }
            }
            case "DEFAULT" -> {
                if ((sourceVal == null || sourceVal.toString().isBlank()) && opMap.containsKey("value")) {
                    targetMap.put(targetField, opMap.get("value"));
                }
            }
            default -> {
                if (sourceVal != null) targetMap.put(targetField, sourceVal);
            }
        }
    }

    private Object applyStringTransform(Object val, String transformType) {
        String str = val.toString();
        return switch (transformType.toUpperCase()) {
            case "UPPERCASE" -> str.toUpperCase();
            case "LOWERCASE" -> str.toLowerCase();
            case "TRIM" -> str.trim();
            default -> val;
        };
    }

    private Object convertType(Object val, String type) {
        return switch (type.toLowerCase()) {
            case "number", "numeric", "double", "float" -> parseNumber(val);
            case "int", "integer" -> {
                try {
                    yield (int) Math.round(Double.parseDouble(val.toString().trim()));
                } catch (Exception e) {
                    yield val;
                }
            }
            case "boolean", "bool" -> Boolean.parseBoolean(val.toString().trim());
            case "string", "text" -> val.toString();
            default -> val;
        };
    }

    private Object parseNumber(Object val) {
        try {
            String s = val.toString().trim();
            if (s.contains(".")) {
                return Double.parseDouble(s);
            }
            return Long.parseLong(s);
        } catch (Exception e) {
            return val;
        }
    }

    private boolean isSystemProperty(String key) {
        return key.equals("label") || key.equals("id") || key.equals("type")
                || key.equals("position") || key.equals("operations")
                || key.equals("mappings") || key.equals("fields") || key.equals("defaults");
    }
}
