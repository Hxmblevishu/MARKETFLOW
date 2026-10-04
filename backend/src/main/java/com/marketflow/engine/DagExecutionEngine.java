package com.marketflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.EdgeDto;
import com.marketflow.dto.NodeDto;
import com.marketflow.dto.WorkflowGraphDto;
import com.marketflow.model.Execution;
import com.marketflow.model.ExecutionStep;
import com.marketflow.model.Workflow;
import com.marketflow.model.enums.ExecutionStatus;
import com.marketflow.model.enums.StepStatus;
import com.marketflow.repository.ExecutionRepository;
import com.marketflow.repository.ExecutionStepRepository;
import com.marketflow.service.GraphValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class DagExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(DagExecutionEngine.class);

    private final List<NodeExecutor> nodeExecutors;
    private final GraphValidationService validationService;
    private final ExecutionRepository executionRepository;
    private final ExecutionStepRepository stepRepository;
    private final ObjectMapper objectMapper;

    public DagExecutionEngine(List<NodeExecutor> nodeExecutors,
                              GraphValidationService validationService,
                              ExecutionRepository executionRepository,
                              ExecutionStepRepository stepRepository,
                              ObjectMapper objectMapper) {
        this.nodeExecutors = nodeExecutors;
        this.validationService = validationService;
        this.executionRepository = executionRepository;
        this.stepRepository = stepRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Execution execute(Workflow workflow, Execution execution, Map<String, Object> inputPayload) {
        if (execution.getId() == null || execution.getId().isBlank()) {
            execution = executionRepository.save(execution);
        }
        log.info("Starting execution [{}] for workflow [{}] - {}", execution.getId(), workflow.getId(), workflow.getName());
        ExecutionContext context = new ExecutionContext(execution.getId(), workflow.getId(), inputPayload);

        try {
            WorkflowGraphDto graph = parseGraphDefinition(workflow.getDefinition());
            validationService.validateWorkflowGraph(graph);

            Map<String, NodeDto> nodeMap = new HashMap<>();
            Map<String, List<EdgeDto>> outEdges = new HashMap<>();

            for (NodeDto node : graph.getNodes()) {
                nodeMap.put(node.getId(), node);
                outEdges.put(node.getId(), new ArrayList<>());
            }

            if (graph.getEdges() != null) {
                for (EdgeDto edge : graph.getEdges()) {
                    outEdges.get(edge.getSource()).add(edge);
                }
            }

            NodeDto triggerNode = validationService.findTriggerNode(graph);
            if (triggerNode == null) {
                throw new IllegalStateException("No trigger node found in workflow definition");
            }

            Queue<NodeDto> queue = new LinkedList<>();
            Set<String> scheduled = new HashSet<>();

            queue.add(triggerNode);
            scheduled.add(triggerNode.getId());

            while (!queue.isEmpty()) {
                NodeDto currentNode = queue.poll();
                Instant stepStart = Instant.now();

                NodeExecutor executor = findExecutor(currentNode.getType());
                NodeExecutionResult result;

                try {
                    result = executor.execute(currentNode, context);
                } catch (Exception ex) {
                    log.error("Execution error at node [{}] {}: {}", currentNode.getId(), currentNode.getLabel(), ex.getMessage(), ex);
                    result = NodeExecutionResult.failed(currentNode.getId(), ex.getMessage());
                }

                Instant stepEnd = Instant.now();
                long durationMs = Duration.between(stepStart, stepEnd).toMillis();

                // Persist step record
                ExecutionStep step = new ExecutionStep();
                step.setExecution(execution);
                step.setNodeId(currentNode.getId());
                step.setNodeType(currentNode.getType());
                step.setStepName(currentNode.getLabel());
                step.setStatus(result.getStatus());
                step.setDurationMs(durationMs);
                step.setStartedAt(stepStart);
                step.setCompletedAt(stepEnd);
                step.setInputData(objectMapper.writeValueAsString(context.getTriggerPayload()));
                step.setOutputData(result.getOutputData() != null ? objectMapper.writeValueAsString(result.getOutputData()) : "{}");
                step.setErrorMessage(result.getErrorMessage());

                stepRepository.save(step);
                execution.addStep(step);

                if (!result.isSuccess()) {
                    execution.setStatus(ExecutionStatus.FAILED);
                    execution.setErrorMessage("Failed at node [" + currentNode.getId() + "]: " + result.getErrorMessage());
                    execution.setCompletedAt(Instant.now());
                    return executionRepository.save(execution);
                }

                context.setNodeOutput(currentNode.getId(), result.getOutputData());

                // Branching traversal
                List<EdgeDto> outgoing = outEdges.getOrDefault(currentNode.getId(), Collections.emptyList());
                for (EdgeDto edge : outgoing) {
                    boolean shouldFollow = shouldFollowEdge(edge, result.getSelectedHandle());
                    if (shouldFollow) {
                        NodeDto nextNode = nodeMap.get(edge.getTarget());
                        if (nextNode != null && scheduled.add(nextNode.getId())) {
                            queue.add(nextNode);
                        }
                    }
                }
            }

            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setOutputData(objectMapper.writeValueAsString(context.getNodeOutputs()));
            execution.setCompletedAt(Instant.now());

        } catch (Exception ex) {
            log.error("Workflow execution failed: {}", ex.getMessage(), ex);
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(ex.getMessage());
            execution.setCompletedAt(Instant.now());
        }

        return executionRepository.save(execution);
    }

    @Async
    public CompletableFuture<Execution> executeAsync(Workflow workflow, Execution execution, Map<String, Object> inputPayload) {
        Execution completed = execute(workflow, execution, inputPayload);
        return CompletableFuture.completedFuture(completed);
    }

    private boolean shouldFollowEdge(EdgeDto edge, String selectedHandle) {
        if (selectedHandle != null && (selectedHandle.equalsIgnoreCase("true") || selectedHandle.equalsIgnoreCase("false"))) {
            // For conditional branching, the edge must explicitly match the evaluated branch
            return edge.getSourceHandle() != null && edge.getSourceHandle().equalsIgnoreCase(selectedHandle);
        }
        if (edge.getSourceHandle() == null || edge.getSourceHandle().isBlank() || edge.getSourceHandle().equalsIgnoreCase("default")) {
            return true;
        }
        if (selectedHandle == null || selectedHandle.equalsIgnoreCase("default")) {
            return true;
        }
        return edge.getSourceHandle().equalsIgnoreCase(selectedHandle);
    }

    private NodeExecutor findExecutor(String nodeType) {
        // Return first executor that explicitly supports this type
        for (NodeExecutor executor : nodeExecutors) {
            if (!(executor.getClass().getSimpleName().equals("DefaultNodeExecutor")) && executor.supports(nodeType)) {
                return executor;
            }
        }
        // Fallback to default
        return nodeExecutors.stream()
                .filter(e -> e.getClass().getSimpleName().equals("DefaultNodeExecutor"))
                .findFirst()
                .orElse(nodeExecutors.get(0));
    }

    private WorkflowGraphDto parseGraphDefinition(String definition) {
        try {
            if (definition == null || definition.isBlank()) {
                return new WorkflowGraphDto();
            }
            return objectMapper.readValue(definition, WorkflowGraphDto.class);
        } catch (Exception e) {
            log.warn("Failed to parse definition as WorkflowGraphDto directly, attempting map conversion: {}", e.getMessage());
            try {
                Map<String, Object> map = objectMapper.readValue(definition, new TypeReference<>() {});
                WorkflowGraphDto graph = new WorkflowGraphDto();
                if (map.containsKey("nodes")) {
                    graph.setNodes(objectMapper.convertValue(map.get("nodes"), new TypeReference<List<NodeDto>>() {}));
                }
                if (map.containsKey("edges")) {
                    graph.setEdges(objectMapper.convertValue(map.get("edges"), new TypeReference<List<EdgeDto>>() {}));
                }
                return graph;
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid workflow definition JSON: " + ex.getMessage(), ex);
            }
        }
    }
}
