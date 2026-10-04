package com.marketflow.engine;

import com.marketflow.dto.EdgeDto;
import com.marketflow.dto.NodeDto;
import com.marketflow.dto.PositionDto;
import com.marketflow.dto.WorkflowGraphDto;
import com.marketflow.exception.InvalidWorkflowGraphException;
import com.marketflow.model.Execution;
import com.marketflow.model.Workflow;
import com.marketflow.repository.ExecutionRepository;
import com.marketflow.repository.ExecutionStepRepository;
import com.marketflow.repository.WorkflowRepository;
import com.marketflow.service.GraphValidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression Test Suite: Edge Cases & System Loopholes
 * 
 * Verifies that previously identified architectural loopholes and edge cases remain fixed:
 * 1. Multiple trigger nodes are rejected by validation
 * 2. Unreachable / orphan nodes are rejected by validation
 * 3. Condition false branches with unset handles are strictly guarded
 * 4. Step outputs properly update context variables without immutability lock
 */
@SpringBootTest
class EngineLoopholeRegressionTests {

    @Autowired
    private GraphValidationService validationService;

    @Autowired
    private DagExecutionEngine dagExecutionEngine;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionStepRepository stepRepository;

    @AfterEach
    void tearDown() {
        stepRepository.deleteAll();
        executionRepository.deleteAll();
        workflowRepository.deleteAll();
    }

    @Test
    @DisplayName("Regression: Validation rejects workflows with multiple trigger nodes")
    void testValidationRejectsMultipleTriggers() {
        NodeDto trigger1 = new NodeDto("trig_1", "trigger", new PositionDto(0, 0), Map.of("label", "Webhook"));
        NodeDto trigger2 = new NodeDto("trig_2", "trigger", new PositionDto(0, 100), Map.of("label", "Manual"));
        NodeDto action = new NodeDto("act_1", "action_slack", new PositionDto(100, 0), Map.of("channel", "#leads"));

        EdgeDto e1 = new EdgeDto("e1", "trig_1", "act_1");
        WorkflowGraphDto graph = new WorkflowGraphDto(List.of(trigger1, trigger2, action), List.of(e1));

        InvalidWorkflowGraphException ex = assertThrows(InvalidWorkflowGraphException.class, 
                () -> validationService.validateWorkflowGraph(graph));
        assertEquals("MULTIPLE_TRIGGERS", ex.getErrorCode());
    }

    @Test
    @DisplayName("Regression: Validation rejects graphs with unreachable orphan nodes")
    void testValidationRejectsUnreachableOrphanNodes() {
        NodeDto trigger = new NodeDto("trig_1", "trigger", new PositionDto(0, 0), Map.of("label", "Start"));
        NodeDto connectedAction = new NodeDto("act_1", "action_slack", new PositionDto(100, 0), Map.of("channel", "#main"));
        NodeDto orphanAction = new NodeDto("orphan_act", "action_email", new PositionDto(200, 200), Map.of("subject", "Dead Node"));

        EdgeDto edge = new EdgeDto("e1", "trig_1", "act_1");
        WorkflowGraphDto graph = new WorkflowGraphDto(List.of(trigger, connectedAction, orphanAction), List.of(edge));

        InvalidWorkflowGraphException ex = assertThrows(InvalidWorkflowGraphException.class, 
                () -> validationService.validateWorkflowGraph(graph));
        assertEquals("UNREACHABLE_NODE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Regression: Condition node does not follow edge when condition is false and handle is unset")
    void testConditionNodeDoesNotFollowEdgeWhenHandleIsUnsetAndConditionIsFalse() {
        String json = """
        {
          "nodes": [
            {"id": "trig", "type": "trigger", "data": {"label": "Start"}},
            {"id": "cond", "type": "condition", "data": {"field": "score", "operator": ">", "value": 70}},
            {"id": "act_slack", "type": "action_slack", "data": {"channel": "#alerts"}}
          ],
          "edges": [
            {"id": "e1", "source": "trig", "target": "cond"},
            {"id": "e2", "source": "cond", "target": "act_slack"}
          ]
        }
        """;

        Workflow wf = workflowRepository.save(new Workflow("Bypass Test", "Tests handle leak", json));
        Execution exec = executionRepository.save(new Execution(wf, "{}"));

        // Score 30 is strictly less than 70 -> condition evaluates to false
        Execution result = dagExecutionEngine.execute(wf, exec, Map.of("score", 30));

        // Exactly 2 steps executed (trigger and condition); act_slack is NOT followed
        assertEquals(2, result.getSteps().size());
    }

    @Test
    @DisplayName("Regression: Step output updates existing context variable for downstream nodes")
    void testStepOutputUpdatesContextVariable() {
        ExecutionContext ctx = new ExecutionContext("e1", "w1", Map.of("status", "TRIGGER_PENDING"));

        // Step 1 processes lead and outputs updated status
        ctx.setNodeOutput("step_1", Map.of("status", "STEP_1_QUALIFIED", "score", 95));

        // Context variable is updated to latest output
        assertEquals("STEP_1_QUALIFIED", ctx.getVariable("status"));
    }
}
