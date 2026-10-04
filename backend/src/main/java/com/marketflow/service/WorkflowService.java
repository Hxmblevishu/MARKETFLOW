package com.marketflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.*;
import com.marketflow.exception.WorkflowNotFoundException;
import com.marketflow.model.Execution;
import com.marketflow.model.Workflow;
import com.marketflow.model.enums.WorkflowStatus;
import com.marketflow.repository.ExecutionRepository;
import com.marketflow.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowRepository workflowRepository;
    private final ExecutionRepository executionRepository;
    private final GraphValidationService validationService;
    private final ObjectMapper objectMapper;

    public WorkflowService(WorkflowRepository workflowRepository,
                           ExecutionRepository executionRepository,
                           GraphValidationService validationService,
                           ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkflowDetailResponse createWorkflow(CreateWorkflowRequest request) {
        log.info("Creating new workflow: '{}'", request.getName());

        List<NodeDto> nodes = request.getNodes() != null ? request.getNodes() : new ArrayList<>();
        List<EdgeDto> edges = request.getEdges() != null ? request.getEdges() : new ArrayList<>();

        WorkflowGraphDto graph = new WorkflowGraphDto(nodes, edges);
        if (!nodes.isEmpty()) {
            validationService.validateWorkflowGraph(graph);
        }

        String definitionJson = serializeGraph(graph);
        Workflow workflow = new Workflow(request.getName(), request.getDescription(), definitionJson);
        Workflow saved = workflowRepository.save(workflow);

        return toWorkflowDetailResponse(saved, nodes, edges);
    }

    @Transactional(readOnly = true)
    public WorkflowDetailResponse getWorkflow(String id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with id: " + id));

        WorkflowGraphDto graph = parseGraphDefinition(workflow.getDefinition());
        return toWorkflowDetailResponse(workflow, graph.getNodes(), graph.getEdges());
    }

    @Transactional(readOnly = true)
    public WorkflowListResponse listWorkflows() {
        List<Workflow> workflows = workflowRepository.findAllByOrderByUpdatedAtDesc();
        List<WorkflowSummaryDto> summaries = workflows.stream()
                .map(w -> new WorkflowSummaryDto(w.getId(), w.getName(), w.getStatus(), w.getUpdatedAt()))
                .collect(Collectors.toList());
        return new WorkflowListResponse(summaries);
    }

    @Transactional
    public WorkflowDetailResponse updateWorkflow(String id, UpdateWorkflowRequest request) {
        log.info("Updating workflow [{}]", id);
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            workflow.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            workflow.setStatus(request.getStatus());
        }

        List<NodeDto> nodes = request.getNodes();
        List<EdgeDto> edges = request.getEdges();

        if (nodes != null && !nodes.isEmpty()) {
            WorkflowGraphDto graph = new WorkflowGraphDto(nodes, edges != null ? edges : new ArrayList<>());
            validationService.validateWorkflowGraph(graph);
            workflow.setDefinition(serializeGraph(graph));
        }

        workflow.setUpdatedAt(Instant.now());
        Workflow updated = workflowRepository.save(workflow);

        WorkflowGraphDto graph = parseGraphDefinition(updated.getDefinition());
        return toWorkflowDetailResponse(updated, graph.getNodes(), graph.getEdges());
    }

    @Transactional
    public void deleteWorkflow(String id) {
        log.info("Deleting workflow [{}]", id);
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with id: " + id));

        // Cascade delete executions
        List<Execution> executions = executionRepository.findByWorkflowIdOrderByCreatedAtDesc(id);
        if (!executions.isEmpty()) {
            executionRepository.deleteAll(executions);
        }

        workflowRepository.delete(workflow);
    }

    @Transactional
    public WorkflowDetailResponse duplicateWorkflow(String id) {
        log.info("Duplicating workflow [{}]", id);
        Workflow original = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with id: " + id));

        String duplicateName = "Copy of " + original.getName();
        Workflow copy = new Workflow(duplicateName, original.getDescription(), original.getDefinition());
        copy.setStatus(WorkflowStatus.DRAFT);
        Workflow saved = workflowRepository.save(copy);

        WorkflowGraphDto graph = parseGraphDefinition(saved.getDefinition());
        return toWorkflowDetailResponse(saved, graph.getNodes(), graph.getEdges());
    }

    public WorkflowGraphDto parseGraphDefinition(String definition) {
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
                log.warn("Failed to parse workflow definition JSON: {}", ex.getMessage());
                return new WorkflowGraphDto();
            }
        }
    }

    private String serializeGraph(WorkflowGraphDto graph) {
        try {
            return objectMapper.writeValueAsString(graph);
        } catch (Exception e) {
            log.error("Failed to serialize graph to JSON: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to serialize workflow graph definition: " + e.getMessage(), e);
        }
    }

    private WorkflowDetailResponse toWorkflowDetailResponse(Workflow workflow, List<NodeDto> nodes, List<EdgeDto> edges) {
        return new WorkflowDetailResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getStatus(),
                nodes != null ? nodes : new ArrayList<>(),
                edges != null ? edges : new ArrayList<>()
        );
    }
}
