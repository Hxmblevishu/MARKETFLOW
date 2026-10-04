package com.marketflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.NodeDto;
import com.marketflow.dto.PositionDto;
import com.marketflow.engine.handler.ConditionHandler;
import com.marketflow.engine.handler.TransformHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Phase 5:
 * Dynamic Data Transformation & Expression Evaluation
 */
class Phase5DynamicDataTests {

    private JsonPathExpressionResolver expressionResolver;
    private TransformHandler transformHandler;
    private ConditionHandler conditionHandler;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        expressionResolver = new JsonPathExpressionResolver();
        transformHandler = new TransformHandler(expressionResolver);
        conditionHandler = new ConditionHandler(expressionResolver);

        Map<String, Object> initialTrigger = Map.of(
                "email", "aarav@marketflow.io",
                "firstName", "Aarav",
                "lastName", "Sharma",
                "score", 85,
                "tier", "ENTERPRISE",
                "company", Map.of("name", "Tech Innovations", "employees", 250)
        );

        context = new ExecutionContext("exec_test_001", "wf_test_001", initialTrigger);
        context.setNodeOutput("step_1", Map.of(
                "output", Map.of("category", "B2B_SaaS", "qualified", true),
                "category", "B2B_SaaS",
                "qualified", true
        ));
    }

    @Test
    @DisplayName("JsonPath: should resolve direct, nested, and Jayway JsonPath expressions")
    void testJsonPathExpressionResolver() {
        // Direct trigger access
        assertEquals("aarav@marketflow.io", expressionResolver.resolveString("{{trigger.email}}", context));
        assertEquals("Tech Innovations", expressionResolver.resolveString("{{trigger.company.name}}", context));

        // Preserving raw types for exact tokens
        Object rawScore = expressionResolver.resolve("{{trigger.score}}", context);
        assertInstanceOf(Number.class, rawScore);
        assertEquals(85, ((Number) rawScore).intValue());

        // Previous step output resolution
        assertEquals("B2B_SaaS", expressionResolver.resolveString("{{step_1.category}}", context));
        assertEquals("B2B_SaaS", expressionResolver.resolveString("{{step_1.output.category}}", context));

        // String interpolation with multiple variables
        String greeting = expressionResolver.resolveString(
                "Lead {{trigger.firstName}} {{trigger.lastName}} scored {{trigger.score}} pts", context);
        assertEquals("Lead Aarav Sharma scored 85 pts", greeting);

        // Jayway syntax with $. prefix
        assertEquals("aarav@marketflow.io", expressionResolver.resolveString("{{$.trigger.email}}", context));
    }

    @Test
    @DisplayName("TransformHandler: should support field extraction, schema mapping and default fallbacks")
    void testTransformHandlerMappingsAndDefaults() {
        NodeDto transformNode = new NodeDto();
        transformNode.setId("node_transform_1");
        transformNode.setType("transform");
        transformNode.setLabel("Lead Data Normalizer");
        transformNode.setData(Map.of(
                "mappings", Map.of(
                        "contactEmail", "{{trigger.email}}",
                        "fullName", "{{trigger.firstName}} {{trigger.lastName}}",
                        "companyName", "{{trigger.company.name}}"
                ),
                "fields", List.of(
                        Map.of("target", "status", "source", "{{trigger.non_existent}}", "defaultValue", "ACTIVE"),
                        Map.of("target", "companyCaps", "source", "{{trigger.company.name}}", "transform", "UPPERCASE"),
                        Map.of("target", "scoreNum", "source", "{{trigger.score}}", "type", "number")
                )
        ));

        NodeExecutionResult result = transformHandler.execute(transformNode, context);

        assertTrue(result.isSuccess());
        assertNotNull(result.getOutputData());

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) result.getOutputData();

        assertEquals("aarav@marketflow.io", output.get("contactEmail"));
        assertEquals("Aarav Sharma", output.get("fullName"));
        assertEquals("Tech Innovations", output.get("companyName"));
        assertEquals("ACTIVE", output.get("status"));
        assertEquals("TECH INNOVATIONS", output.get("companyCaps"));
        assertEquals(85L, ((Number) output.get("scoreNum")).longValue());
    }

    @Test
    @DisplayName("TransformHandler: should support string transformations (UPPERCASE, LOWERCASE, TRIM)")
    void testTransformHandlerStringOperations() {
        NodeDto opNode = new NodeDto();
        opNode.setId("node_op");
        opNode.setType("data_transform");
        opNode.setData(Map.of(
                "operations", List.of(
                        Map.of("target", "cleanEmail", "source", "  AARAV@MARKETFLOW.IO  ", "operation", "TRIM"),
                        Map.of("target", "lowerEmail", "source", "AARAV@MARKETFLOW.IO", "operation", "LOWERCASE"),
                        Map.of("target", "upperTier", "source", "enterprise", "operation", "UPPERCASE"),
                        Map.of("target", "replaced", "source", "hello_world", "operation", "REPLACE", "find", "_", "replacement", " ")
                )
        ));

        NodeExecutionResult result = transformHandler.execute(opNode, context);
        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) result.getOutputData();
        assertEquals("AARAV@MARKETFLOW.IO", output.get("cleanEmail"));
        assertEquals("aarav@marketflow.io", output.get("lowerEmail"));
        assertEquals("ENTERPRISE", output.get("upperTier"));
        assertEquals("hello world", output.get("replaced"));
    }

    @Test
    @DisplayName("ConditionHandler: should evaluate numeric comparison operators (>, <, >=, <=, ==, !=)")
    void testConditionNumericOperators() {
        assertTrue(conditionHandler.evaluateCondition(85, ">", 70));
        assertTrue(conditionHandler.evaluateCondition(85, ">=", 85));
        assertFalse(conditionHandler.evaluateCondition(85, "<", 50));
        assertTrue(conditionHandler.evaluateCondition(50, "<=", 50));
        assertTrue(conditionHandler.evaluateCondition(100, "==", 100));
        assertTrue(conditionHandler.evaluateCondition(100, "!=", 99));
    }

    @Test
    @DisplayName("ConditionHandler: should evaluate REGEX, CONTAINS, NOT_CONTAINS, STARTS_WITH, and IN")
    void testConditionAdvancedOperators() {
        // REGEX
        assertTrue(conditionHandler.evaluateCondition("aarav@marketflow.io", "REGEX", "^[a-z]+@marketflow\\.io$"));
        assertFalse(conditionHandler.evaluateCondition("invalid-email", "REGEX", "^[a-z]+@marketflow\\.io$"));

        // CONTAINS & NOT_CONTAINS
        assertTrue(conditionHandler.evaluateCondition("Enterprise Tier Customer", "CONTAINS", "enterprise"));
        assertTrue(conditionHandler.evaluateCondition("Enterprise Tier Customer", "NOT_CONTAINS", "starter"));

        // STARTS_WITH & ENDS_WITH
        assertTrue(conditionHandler.evaluateCondition("Marketflow Automation", "STARTS_WITH", "Market"));
        assertTrue(conditionHandler.evaluateCondition("report.pdf", "ENDS_WITH", ".pdf"));

        // IN & NOT_IN
        assertTrue(conditionHandler.evaluateCondition("ENTERPRISE", "IN", "STARTUP, PRO, ENTERPRISE"));
        assertFalse(conditionHandler.evaluateCondition("FREE", "IN", "STARTUP, PRO, ENTERPRISE"));
        assertTrue(conditionHandler.evaluateCondition("FREE", "NOT_IN", "STARTUP, PRO, ENTERPRISE"));

        // IS_EMPTY & IS_NOT_EMPTY
        assertTrue(conditionHandler.evaluateCondition("", "IS_EMPTY", null));
        assertTrue(conditionHandler.evaluateCondition(null, "IS_EMPTY", null));
        assertTrue(conditionHandler.evaluateCondition("data", "IS_NOT_EMPTY", null));
    }

    @Test
    @DisplayName("ConditionHandler: should execute dynamic condition node and direct flow via true/false branch")
    void testConditionNodeExecutionWithContextVariables() {
        NodeDto conditionNode = new NodeDto();
        conditionNode.setId("node_cond_1");
        conditionNode.setType("condition");
        conditionNode.setData(Map.of(
                "field", "{{trigger.score}}",
                "operator", ">=",
                "value", 80
        ));

        NodeExecutionResult result = conditionHandler.execute(conditionNode, context);

        assertTrue(result.isSuccess());
        assertEquals("true", result.getSelectedHandle());

        // Test false branch with high threshold
        conditionNode.setData(Map.of(
                "field", "{{trigger.score}}",
                "operator", ">=",
                "value", 95
        ));

        NodeExecutionResult falseResult = conditionHandler.execute(conditionNode, context);
        assertTrue(falseResult.isSuccess());
        assertEquals("false", falseResult.getSelectedHandle());
    }

    @Test
    @DisplayName("ConditionHandler: should support compound rules with AND/OR logic")
    void testCompoundConditionRules() {
        NodeDto compoundNode = new NodeDto();
        compoundNode.setId("node_compound");
        compoundNode.setType("condition");
        compoundNode.setData(Map.of(
                "logicalOperator", "AND",
                "rules", List.of(
                        Map.of("field", "{{trigger.score}}", "operator", ">=", "value", 70),
                        Map.of("field", "{{trigger.tier}}", "operator", "==", "value", "ENTERPRISE")
                )
        ));

        NodeExecutionResult result = conditionHandler.execute(compoundNode, context);
        assertTrue(result.isSuccess());
        assertEquals("true", result.getSelectedHandle());

        // Test OR logic
        compoundNode.setData(Map.of(
                "logicalOperator", "OR",
                "rules", List.of(
                        Map.of("field", "{{trigger.score}}", "operator", ">=", "value", 999), // false
                        Map.of("field", "{{trigger.tier}}", "operator", "==", "value", "ENTERPRISE") // true
                )
        ));

        NodeExecutionResult orResult = conditionHandler.execute(compoundNode, context);
        assertEquals("true", orResult.getSelectedHandle());
    }
}
