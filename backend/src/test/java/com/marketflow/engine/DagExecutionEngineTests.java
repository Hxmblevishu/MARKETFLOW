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
}
