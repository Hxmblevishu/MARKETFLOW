package com.marketflow.service;

import com.marketflow.dto.EdgeDto;
import com.marketflow.dto.NodeDto;
import com.marketflow.dto.PositionDto;
import com.marketflow.dto.WorkflowGraphDto;
import com.marketflow.exception.InvalidWorkflowGraphException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphValidationServiceTests {

    private GraphValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new GraphValidationService();
    }

    @Test
    @DisplayName("Should successfully validate a valid DAG workflow")
    void testValidDagWorkflow() {
        NodeDto trigger = new NodeDto("node_1", "trigger", new PositionDto(100, 100), Map.of("label", "Instagram Lead"));
        NodeDto condition = new NodeDto("node_2", "condition", new PositionDto(100, 200), Map.of("field", "score", "operator", ">", "value", 70));
        NodeDto action = new NodeDto("node_3", "action_slack", new PositionDto(100, 300), Map.of("label", "Notify Sales"));

        EdgeDto edge1 = new EdgeDto("e1", "node_1", "node_2");
        EdgeDto edge2 = new EdgeDto("e2", "node_2", "node_3", "true");

        WorkflowGraphDto graph = new WorkflowGraphDto(List.of(trigger, condition, action), List.of(edge1, edge2));

        assertDoesNotThrow(() -> validationService.validateWorkflowGraph(graph));
    }

    @Test
    @DisplayName("Should throw MISSING_TRIGGER when no trigger node exists")
    void testMissingTriggerThrowsException() {
        NodeDto action1 = new NodeDto("node_1", "action", new PositionDto(100, 100), Map.of());
        NodeDto action2 = new NodeDto("node_2", "action", new PositionDto(100, 200), Map.of());
        EdgeDto edge1 = new EdgeDto("e1", "node_1", "node_2");

        WorkflowGraphDto graph = new WorkflowGraphDto(List.of(action1, action2), List.of(edge1));

        InvalidWorkflowGraphException ex = assertThrows(InvalidWorkflowGraphException.class, 
                () -> validationService.validateWorkflowGraph(graph));
        assertEquals("MISSING_TRIGGER", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should detect circular dependency/cycle (A -> B -> C -> A)")
    void testCycleDetectionThrowsException() {
        NodeDto trigger = new NodeDto("node_1", "trigger", new PositionDto(100, 100), Map.of());
        NodeDto nodeA = new NodeDto("node_2", "action", new PositionDto(100, 200), Map.of());
        NodeDto nodeB = new NodeDto("node_3", "condition", new PositionDto(100, 300), Map.of());
        NodeDto nodeC = new NodeDto("node_4", "action", new PositionDto(100, 400), Map.of());

        EdgeDto e1 = new EdgeDto("e1", "node_1", "node_2");
        EdgeDto e2 = new EdgeDto("e2", "node_2", "node_3");
        EdgeDto e3 = new EdgeDto("e3", "node_3", "node_4");
        EdgeDto cycleEdge = new EdgeDto("e4", "node_4", "node_2"); // Creates cycle 2 -> 3 -> 4 -> 2

        WorkflowGraphDto graph = new WorkflowGraphDto(List.of(trigger, nodeA, nodeB, nodeC), List.of(e1, e2, e3, cycleEdge));

        InvalidWorkflowGraphException ex = assertThrows(InvalidWorkflowGraphException.class, 
                () -> validationService.validateWorkflowGraph(graph));
        assertEquals("CYCLE_DETECTED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject self-loop edge (A -> A)")
    void testSelfLoopThrowsException() {
        NodeDto trigger = new NodeDto("node_1", "trigger", new PositionDto(100, 100), Map.of());
        EdgeDto selfEdge = new EdgeDto("e1", "node_1", "node_1");

        WorkflowGraphDto graph = new WorkflowGraphDto(List.of(trigger), List.of(selfEdge));

        InvalidWorkflowGraphException ex = assertThrows(InvalidWorkflowGraphException.class, 
                () -> validationService.validateWorkflowGraph(graph));
        assertEquals("SELF_LOOP_DETECTED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject edge targeting non-existent node")
    void testInvalidTargetNodeThrowsException() {
        NodeDto trigger = new NodeDto("node_1", "trigger", new PositionDto(100, 100), Map.of());
        EdgeDto badEdge = new EdgeDto("e1", "node_1", "ghost_node");

        WorkflowGraphDto graph = new WorkflowGraphDto(List.of(trigger), List.of(badEdge));

        InvalidWorkflowGraphException ex = assertThrows(InvalidWorkflowGraphException.class, 
                () -> validationService.validateWorkflowGraph(graph));
        assertEquals("INVALID_EDGE_TARGET", ex.getErrorCode());
    }
}
