package com.marketflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.model.Execution;
import com.marketflow.model.ExecutionStep;
import com.marketflow.model.Workflow;
import com.marketflow.model.enums.ExecutionStatus;
import com.marketflow.model.enums.StepStatus;
import com.marketflow.repository.ExecutionRepository;
import com.marketflow.repository.WorkflowRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DagExecutionEngineTests {

    @Autowired
    private DagExecutionEngine dagExecutionEngine;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private com.marketflow.repository.ExecutionStepRepository stepRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Workflow qualificationWorkflow;

    @AfterEach
    void tearDown() {
        stepRepository.deleteAll();
        executionRepository.deleteAll();
        workflowRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        String leadGraphJson = """
        {
          "nodes": [
            {
              "id": "node_trigger",
              "type": "trigger",
              "position": {"x": 100, "y": 100},
              "data": {"label": "New Lead Ingestion"}
            },
            {
              "id": "node_condition",
              "type": "condition",
              "position": {"x": 100, "y": 200},
              "data": {"field": "score", "operator": ">", "value": 70}
            },
            {
              "id": "node_slack",
              "type": "action_slack",
              "position": {"x": 50, "y": 300},
              "data": {"channel": "#hot-leads", "label": "Slack Alert"}
            },
            {
              "id": "node_email",
              "type": "action_email",
              "position": {"x": 250, "y": 300},
              "data": {"subject": "Nurture Campaign", "label": "Email Nurture"}
            }
          ],
          "edges": [
            {
              "id": "e_trigger_condition",
              "source": "node_trigger",
              "target": "node_condition"
            },
            {
              "id": "e_condition_slack",
              "source": "node_condition",
              "target": "node_slack",
              "sourceHandle": "true"
            },
            {
              "id": "e_condition_email",
              "source": "node_condition",
              "target": "node_email",
              "sourceHandle": "false"
            }
          ]
        }
        """;

        qualificationWorkflow = new Workflow("Lead Qualification Workflow", "Routes leads based on score", leadGraphJson);
        qualificationWorkflow = workflowRepository.save(qualificationWorkflow);
    }

    @Test
    @DisplayName("Should route high-score lead (score = 85) to Slack and skip Email")
    void testBranchingHighScoreLeadToSlack() {
        Map<String, Object> input = Map.of(
                "leadName", "Aarav",
                "email", "aarav@example.com",
                "score", 85
        );

        Execution execution = new Execution(qualificationWorkflow, "{}");
        execution = executionRepository.save(execution);

        Execution result = dagExecutionEngine.execute(qualificationWorkflow, execution, input);

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        assertNull(result.getErrorMessage());

        List<ExecutionStep> steps = result.getSteps();
        assertEquals(3, steps.size(), "Should execute exactly 3 steps: trigger, condition, and slack");

        assertEquals("node_trigger", steps.get(0).getNodeId());
        assertEquals(StepStatus.COMPLETED, steps.get(0).getStatus());

        assertEquals("node_condition", steps.get(1).getNodeId());
        assertEquals(StepStatus.COMPLETED, steps.get(1).getStatus());

        assertEquals("node_slack", steps.get(2).getNodeId());
        assertEquals(StepStatus.COMPLETED, steps.get(2).getStatus());
        assertTrue(steps.get(2).getOutputData().contains("#hot-leads"));
    }

    @Test
    @DisplayName("Should route low-score lead (score = 45) to Email and skip Slack")
    void testBranchingLowScoreLeadToEmail() {
        Map<String, Object> input = Map.of(
                "leadName", "Priya",
                "email", "priya@example.com",
                "score", 45
        );

        Execution execution = new Execution(qualificationWorkflow, "{}");
        execution = executionRepository.save(execution);

        Execution result = dagExecutionEngine.execute(qualificationWorkflow, execution, input);

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());

        List<ExecutionStep> steps = result.getSteps();
        assertEquals(3, steps.size(), "Should execute exactly 3 steps: trigger, condition, and email");

        assertEquals("node_trigger", steps.get(0).getNodeId());
        assertEquals("node_condition", steps.get(1).getNodeId());
        assertEquals("node_email", steps.get(2).getNodeId());
        assertEquals(StepStatus.COMPLETED, steps.get(2).getStatus());
        assertTrue(steps.get(2).getOutputData().contains("Nurture Campaign"));
    }

    @Test
    @DisplayName("Should execute asynchronously via CompletableFuture")
    void testAsyncExecution() throws Exception {
        Map<String, Object> input = Map.of(
                "leadName", "Dev",
                "email", "dev@example.com",
                "score", 92
        );

        Execution execution = new Execution(qualificationWorkflow, "{}");
        execution = executionRepository.save(execution);

        CompletableFuture<Execution> future = dagExecutionEngine.executeAsync(qualificationWorkflow, execution, input);
        Execution result = future.get();

        assertNotNull(result);
        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
        assertEquals(3, result.getSteps().size());
    }

    @Test
    @DisplayName("Should fail execution when workflow graph contains an invalid cycle")
    void testCycleWorkflowFailsExecution() {
        String cycleGraphJson = """
        {
          "nodes": [
            {
              "id": "node_trigger",
              "type": "trigger",
              "data": {"label": "Trigger"}
            },
            {
              "id": "node_a",
              "type": "action",
              "data": {"label": "A"}
            },
            {
              "id": "node_b",
              "type": "action",
              "data": {"label": "B"}
            }
          ],
          "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_a"},
            {"id": "e2", "source": "node_a", "target": "node_b"},
            {"id": "e3", "source": "node_b", "target": "node_a"}
          ]
        }
        """;

        Workflow cycleWf = workflowRepository.save(new Workflow("Cyclic Workflow", "Invalid cycle", cycleGraphJson));
        Execution execution = executionRepository.save(new Execution(cycleWf, "{}"));

        Execution result = dagExecutionEngine.execute(cycleWf, execution, Map.of());

        assertEquals(ExecutionStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("circular dependency/cycle") || result.getErrorMessage().contains("CYCLE_DETECTED"));
    }

    @Test
    @DisplayName("Should handle malformed JSON workflow definition gracefully without crashing")
    void testMalformedJsonDefinitionFailsGracefully() {
        Workflow malformedWf = workflowRepository.save(new Workflow("Broken JSON", "Bad syntax", "{ not valid json at all"));
        Execution execution = executionRepository.save(new Execution(malformedWf, "{}"));

        Execution result = dagExecutionEngine.execute(malformedWf, execution, Map.of("key", "val"));

        assertEquals(ExecutionStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().toLowerCase().contains("invalid workflow definition") || result.getErrorMessage().contains("JSON"));
    }

    @Test
    @DisplayName("Should handle empty workflow definition gracefully")
    void testEmptyDefinitionFailsGracefully() {
        Workflow emptyWf = workflowRepository.save(new Workflow("Empty Wf", "No nodes", ""));
        Execution execution = executionRepository.save(new Execution(emptyWf, "{}"));

        Execution result = dagExecutionEngine.execute(emptyWf, execution, Map.of());

        assertEquals(ExecutionStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should execute fan-out workflow where one trigger branches to multiple actions")
    void testFanOutMultipleBranches() {
        String fanOutJson = """
        {
          "nodes": [
            {
              "id": "node_trigger",
              "type": "trigger",
              "data": {"label": "New Customer"}
            },
            {
              "id": "node_slack",
              "type": "action_slack",
              "data": {"channel": "#welcome"}
            },
            {
              "id": "node_email",
              "type": "action_email",
              "data": {"subject": "Welcome"}
            }
          ],
          "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_slack"},
            {"id": "e2", "source": "node_trigger", "target": "node_email"}
          ]
        }
        """;

        Workflow fanOutWf = workflowRepository.save(new Workflow("Fan-Out Workflow", "Parallel notifications", fanOutJson));
        Execution execution = executionRepository.save(new Execution(fanOutWf, "{}"));

        Execution result = dagExecutionEngine.execute(fanOutWf, execution, Map.of("customer", "John"));

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
        assertEquals(3, result.getSteps().size(), "Should execute trigger, slack action, and email action");
    }

    @Test
    @DisplayName("Should correctly evaluate string CONTAINS condition operator")
    void testStringContainsConditionOperator() {
        String stringCondJson = """
        {
          "nodes": [
            {
              "id": "node_trigger",
              "type": "trigger",
              "data": {"label": "Web Visit"}
            },
            {
              "id": "node_cond",
              "type": "condition",
              "data": {"field": "source", "operator": "CONTAINS", "value": "google"}
            },
            {
              "id": "node_action",
              "type": "action_crm",
              "data": {"label": "Log Google Lead"}
            }
          ],
          "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_cond"},
            {"id": "e2", "source": "node_cond", "target": "node_action", "sourceHandle": "true"}
          ]
        }
        """;

        Workflow wf = workflowRepository.save(new Workflow("Google Source Workflow", "Filter google", stringCondJson));

        // Matching payload
        Execution execMatch = executionRepository.save(new Execution(wf, "{}"));
        Execution matchResult = dagExecutionEngine.execute(wf, execMatch, Map.of("source", "https://google.com/search"));
        assertEquals(ExecutionStatus.COMPLETED, matchResult.getStatus());
        assertEquals(3, matchResult.getSteps().size());

        // Non-matching payload
        Execution execNoMatch = executionRepository.save(new Execution(wf, "{}"));
        Execution noMatchResult = dagExecutionEngine.execute(wf, execNoMatch, Map.of("source", "direct-visit"));
        assertEquals(ExecutionStatus.COMPLETED, noMatchResult.getStatus());
        assertEquals(2, noMatchResult.getSteps().size(), "Should not execute node_action when condition is false");
    }

    @Test
    @DisplayName("Should safely handle missing condition field without NullPointerException")
    void testMissingConditionFieldSafelyEvaluatesFalse() {
        String missingFieldJson = """
        {
          "nodes": [
            {
              "id": "node_trigger",
              "type": "trigger",
              "data": {"label": "Event"}
            },
            {
              "id": "node_cond",
              "type": "condition",
              "data": {"field": "non_existent_key", "operator": "==", "value": "vip"}
            },
            {
              "id": "node_action",
              "type": "action",
              "data": {"label": "VIP Action"}
            }
          ],
          "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_cond"},
            {"id": "e2", "source": "node_cond", "target": "node_action", "sourceHandle": "true"}
          ]
        }
        """;

        Workflow wf = workflowRepository.save(new Workflow("Missing Key Test", "Test null handling", missingFieldJson));
        Execution exec = executionRepository.save(new Execution(wf, "{}"));

        Execution result = dagExecutionEngine.execute(wf, exec, Map.of("some_other_field", 123));

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
        assertEquals(2, result.getSteps().size(), "Node action should not execute because missing field evaluated to false");
    }
}
