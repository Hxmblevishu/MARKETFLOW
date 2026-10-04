package com.marketflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.*;
import com.marketflow.engine.DagExecutionEngine;
import com.marketflow.exception.ExecutionNotFoundException;
import com.marketflow.exception.WorkflowNotFoundException;
import com.marketflow.model.Execution;
import com.marketflow.model.ExecutionStep;
import com.marketflow.model.Workflow;
import com.marketflow.model.enums.ExecutionStatus;
import com.marketflow.model.enums.WorkflowStatus;
import com.marketflow.repository.ExecutionRepository;
import com.marketflow.repository.ExecutionStepRepository;
import com.marketflow.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionRepository executionRepository;
    private final ExecutionStepRepository stepRepository;
    private final WorkflowRepository workflowRepository;
    private final DagExecutionEngine dagExecutionEngine;
    private final GraphValidationService validationService;
    private final ObjectMapper objectMapper;

    public ExecutionService(ExecutionRepository executionRepository,
                            ExecutionStepRepository stepRepository,
                            WorkflowRepository workflowRepository,
                            DagExecutionEngine dagExecutionEngine,
                            GraphValidationService validationService,
                            ObjectMapper objectMapper) {
        this.executionRepository = executionRepository;
        this.stepRepository = stepRepository;
        this.workflowRepository = workflowRepository;
        this.dagExecutionEngine = dagExecutionEngine;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExecuteWorkflowResponse executeWorkflow(String workflowId, ExecuteWorkflowRequest request) {
        log.info("Triggering synchronous execution for workflow [{}]", workflowId);
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with id: " + workflowId));

        if (workflow.getStatus() == WorkflowStatus.PAUSED) {
            throw new IllegalStateException("Cannot execute a paused workflow: " + workflowId);
        }
        if (workflow.getStatus() == WorkflowStatus.DRAFT) {
            throw new IllegalStateException("Cannot execute a draft workflow: " + workflowId);
        }

        // Pre-validate graph before persisting execution record
        WorkflowGraphDto graph = parseGraphDefinition(workflow.getDefinition());
        validationService.validateWorkflowGraph(graph);

        Map<String, Object> input = (request != null && request.getInput() != null) ? request.getInput() : new HashMap<>();
        String inputJson = serializeJson(input);

        Execution execution = new Execution(workflow, inputJson);
        execution = executionRepository.save(execution);

        Execution completed = dagExecutionEngine.execute(workflow, execution, input);

        return new ExecuteWorkflowResponse(completed.getId(), workflow.getId(), completed.getStatus());
    }

    public ExecuteWorkflowResponse executeWorkflowAsync(String workflowId, ExecuteWorkflowRequest request) {
        log.info("Triggering asynchronous execution for workflow [{}]", workflowId);
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with id: " + workflowId));

        if (workflow.getStatus() == WorkflowStatus.PAUSED) {
            throw new IllegalStateException("Cannot execute a paused workflow: " + workflowId);
        }
        if (workflow.getStatus() == WorkflowStatus.DRAFT) {
            throw new IllegalStateException("Cannot execute a draft workflow: " + workflowId);
        }

        // Pre-validate graph before persisting execution record
        WorkflowGraphDto graph = parseGraphDefinition(workflow.getDefinition());
        validationService.validateWorkflowGraph(graph);

        Map<String, Object> input = (request != null && request.getInput() != null) ? request.getInput() : new HashMap<>();
        String inputJson = serializeJson(input);

        Execution execution = new Execution(workflow, inputJson);
        execution = executionRepository.save(execution);

        dagExecutionEngine.executeAsync(workflow, execution, input);

        return new ExecuteWorkflowResponse(execution.getId(), workflow.getId(), ExecutionStatus.RUNNING);
    }

    @Transactional(readOnly = true)
    public ExecutionDetailResponse getExecution(String executionId) {
        Execution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ExecutionNotFoundException("Execution not found with id: " + executionId));

        List<ExecutionStep> steps = stepRepository.findByExecutionIdOrderByStartedAtAsc(executionId);
        return toExecutionDetailResponse(execution, steps);
    }

    @Transactional(readOnly = true)
    public List<ExecutionDetailResponse> listExecutions(String workflowId) {
        List<Execution> executions;
        if (workflowId != null && !workflowId.isBlank()) {
            executions = executionRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
        } else {
            executions = executionRepository.findTop20ByOrderByCreatedAtDesc();
        }

        return executions.stream()
                .map(e -> {
                    List<ExecutionStep> steps = stepRepository.findByExecutionIdOrderByStartedAtAsc(e.getId());
                    return toExecutionDetailResponse(e, steps);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ExecuteWorkflowResponse retryExecution(String executionId) {
        log.info("Retrying execution [{}]", executionId);
        Execution previous = executionRepository.findById(executionId)
                .orElseThrow(() -> new ExecutionNotFoundException("Execution not found with id: " + executionId));

        Map<String, Object> originalInput = deserializeJson(previous.getInputData());
        ExecuteWorkflowRequest retryRequest = new ExecuteWorkflowRequest(originalInput);

        return executeWorkflow(previous.getWorkflow().getId(), retryRequest);
    }

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getMetrics() {
        long totalWorkflows = workflowRepository.count();
        long activeWorkflows = workflowRepository.countByStatus(WorkflowStatus.ACTIVE);
        long totalExecutions = executionRepository.count();
        long successfulExecutions = executionRepository.countByStatus(ExecutionStatus.COMPLETED);
        long failedExecutions = executionRepository.countByStatus(ExecutionStatus.FAILED);

        double rate = totalExecutions > 0 ? ((double) successfulExecutions / totalExecutions) * 100.0 : 0.0;
        double roundedRate = Math.round(rate * 10.0) / 10.0;

        return new DashboardMetricsResponse(totalWorkflows, activeWorkflows, totalExecutions,
                successfulExecutions, failedExecutions, roundedRate);
    }

    private ExecutionDetailResponse toExecutionDetailResponse(Execution execution, List<ExecutionStep> steps) {
        ExecutionDetailResponse response = new ExecutionDetailResponse();
        response.setId(execution.getId());
        response.setWorkflowId(execution.getWorkflow() != null ? execution.getWorkflow().getId() : null);
        response.setStatus(execution.getStatus());
        response.setInput(deserializeJson(execution.getInputData()));
        response.setOutput(deserializeJson(execution.getOutputData()));
        response.setErrorMessage(execution.getErrorMessage());
        response.setStartedAt(execution.getStartedAt());
        response.setCompletedAt(execution.getCompletedAt());

        List<ExecutionStepDto> stepDtos = (steps != null ? steps : Collections.<ExecutionStep>emptyList()).stream()
                .map(this::toExecutionStepDto)
                .collect(Collectors.toList());
        response.setSteps(stepDtos);

        return response;
    }

    private ExecutionStepDto toExecutionStepDto(ExecutionStep step) {
        ExecutionStepDto dto = new ExecutionStepDto(step.getNodeId(), step.getStatus());
        dto.setNodeType(step.getNodeType());
        dto.setStepName(step.getStepName());
        dto.setInputData(deserializeJson(step.getInputData()));
        dto.setOutputData(deserializeJson(step.getOutputData()));
        dto.setErrorMessage(step.getErrorMessage());
        dto.setDurationMs(step.getDurationMs());
        dto.setStartedAt(step.getStartedAt());
        dto.setCompletedAt(step.getCompletedAt());
        return dto;
    }

    private String serializeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private Map<String, Object> deserializeJson(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private WorkflowGraphDto parseGraphDefinition(String definition) {
        if (definition == null || definition.isBlank()) {
            return new WorkflowGraphDto();
        }
        try {
            return objectMapper.readValue(definition, WorkflowGraphDto.class);
        } catch (Exception e) {
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
                return new WorkflowGraphDto();
            }
        }
    }
}
