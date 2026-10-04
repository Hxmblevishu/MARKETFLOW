package com.marketflow.service;

import com.marketflow.dto.EdgeDto;
import com.marketflow.dto.NodeDto;
import com.marketflow.dto.WorkflowGraphDto;
import com.marketflow.exception.InvalidWorkflowGraphException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphValidationService {

    public void validateWorkflowGraph(WorkflowGraphDto graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            throw new InvalidWorkflowGraphException("EMPTY_GRAPH", "Workflow graph must contain at least one node.");
        }

        List<NodeDto> nodes = graph.getNodes();
        List<EdgeDto> edges = graph.getEdges() != null ? graph.getEdges() : Collections.emptyList();

        // 1. Check duplicate node IDs
        Set<String> nodeIds = new HashSet<>();
        for (NodeDto node : nodes) {
            if (node.getId() == null || node.getId().isBlank()) {
                throw new InvalidWorkflowGraphException("INVALID_NODE_ID", "All nodes must have a non-empty ID.");
            }
            if (!nodeIds.add(node.getId())) {
                throw new InvalidWorkflowGraphException("DUPLICATE_NODE_ID", "Duplicate node ID found: " + node.getId());
            }
        }

        // 2. Validate Trigger Node exists
        List<NodeDto> triggerNodes = nodes.stream()
                .filter(this::isTriggerNode)
                .toList();

        if (triggerNodes.isEmpty()) {
            throw new InvalidWorkflowGraphException("MISSING_TRIGGER", "Workflow must contain at least one trigger node.");
        }

        // 3. Validate Edges connect existing nodes
        for (EdgeDto edge : edges) {
            if (edge.getSource() == null || !nodeIds.contains(edge.getSource())) {
                throw new InvalidWorkflowGraphException("INVALID_EDGE_SOURCE", 
                        "Edge source does not reference a valid node: " + edge.getSource());
            }
            if (edge.getTarget() == null || !nodeIds.contains(edge.getTarget())) {
                throw new InvalidWorkflowGraphException("INVALID_EDGE_TARGET", 
                        "Edge target does not reference a valid node: " + edge.getTarget());
            }
            if (edge.getSource().equals(edge.getTarget())) {
                throw new InvalidWorkflowGraphException("SELF_LOOP_DETECTED", 
                        "Node " + edge.getSource() + " cannot have an edge connecting to itself.");
            }
        }

        // 4. Validate Trigger has no incoming edges
        Set<String> triggerIds = triggerNodes.stream().map(NodeDto::getId).collect(Collectors.toSet());
        for (EdgeDto edge : edges) {
            if (triggerIds.contains(edge.getTarget())) {
                throw new InvalidWorkflowGraphException("TRIGGER_HAS_INCOMING_EDGE", 
                        "Trigger node " + edge.getTarget() + " cannot have incoming edges.");
            }
        }

        // 5. Detect Cycles (Ensure DAG integrity using Kahn's algorithm)
        detectCycles(nodes, edges);
    }

    public NodeDto findTriggerNode(WorkflowGraphDto graph) {
        if (graph == null || graph.getNodes() == null) return null;
        return graph.getNodes().stream()
                .filter(this::isTriggerNode)
                .findFirst()
                .orElse(null);
    }

    public boolean isTriggerNode(NodeDto node) {
        if (node == null || node.getType() == null) return false;
        String type = node.getType().toLowerCase();
        return type.equals("trigger") || type.startsWith("trigger_") || type.contains("trigger");
    }

    private void detectCycles(List<NodeDto> nodes, List<EdgeDto> edges) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();

        for (NodeDto node : nodes) {
            inDegree.put(node.getId(), 0);
            adj.put(node.getId(), new ArrayList<>());
        }

        for (EdgeDto edge : edges) {
            adj.get(edge.getSource()).add(edge.getTarget());
            inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int visitedCount = 0;
        while (!queue.isEmpty()) {
            String u = queue.poll();
            visitedCount++;

            for (String v : adj.get(u)) {
                inDegree.put(v, inDegree.get(v) - 1);
                if (inDegree.get(v) == 0) {
                    queue.add(v);
                }
            }
        }

        if (visitedCount < nodes.size()) {
            throw new InvalidWorkflowGraphException("CYCLE_DETECTED", 
                    "Workflow graph contains an invalid circular dependency/cycle.");
        }
    }
}
