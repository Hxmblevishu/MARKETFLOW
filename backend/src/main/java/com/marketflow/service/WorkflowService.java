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
import com.marketflow.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

        // Multi-Tenant Isolation: Associate workflow with authenticated user
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            workflow.setUserId(currentUserId);
        }

        Workflow saved = workflowRepository.save(workflow);
        return toWorkflowDetailResponse(saved, nodes, edges);
    }

    @Transactional(readOnly = true)
    public WorkflowDetailResponse getWorkflow(String id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with id: " + id));

        // IDOR Defense: verify ownership
        checkOwnership(workflow, "read");

        WorkflowGraphDto graph = parseGraphDefinition(workflow.getDefinition());
        return toWorkflowDetailResponse(workflow, graph.getNodes(), graph.getEdges());
    }

    @Transactional(readOnly = true)
    public WorkflowListResponse listWorkflows() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        List<Workflow> workflows;
        if (isAdmin || currentUserId == null) {
            // Admins or unauthenticated (if public mode) see all
            workflows = workflowRepository.findAllByOrderByUpdatedAtDesc();
        } else {
            // Regular user sees their owned workflows + public system templates (userId == null)
            workflows = workflowRepository.findAccessibleWorkflows(currentUserId);
        }

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

        // IDOR Defense: verify caller owns this workflow
        checkOwnership(workflow, "modify");

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

        // IDOR Defense: verify caller owns this workflow
        checkOwnership(workflow, "delete");

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

        checkOwnership(original, "duplicate");

        String duplicateName = "Copy of " + original.getName();
        Workflow copy = new Workflow(duplicateName, original.getDescription(), original.getDefinition());
        copy.setStatus(WorkflowStatus.DRAFT);
        copy.setUserId(SecurityUtils.getCurrentUserId());

        Workflow saved = workflowRepository.save(copy);

        WorkflowGraphDto graph = parseGraphDefinition(saved.getDefinition());
        return toWorkflowDetailResponse(saved, graph.getNodes(), graph.getEdges());
    }

    @Transactional(readOnly = true)
    public WorkflowExportDto exportWorkflow(String id) {
        log.info("Exporting workflow [{}]", id);
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with id: " + id));

        checkOwnership(workflow, "export");

        WorkflowGraphDto graph = parseGraphDefinition(workflow.getDefinition());
        String checksum = calculateSha256(workflow.getDefinition());

        return new WorkflowExportDto(
                "1.0",
                Instant.now().toString(),
                workflow.getName(),
                workflow.getDescription(),
                graph.getNodes(),
                graph.getEdges(),
                checksum
        );
    }

    @Transactional
    public WorkflowDetailResponse importWorkflow(WorkflowExportDto exportDto) {
        log.info("Importing workflow bundle: '{}'", exportDto.getName());
        List<NodeDto> nodes = exportDto.getNodes() != null ? exportDto.getNodes() : new ArrayList<>();
        List<EdgeDto> edges = exportDto.getEdges() != null ? exportDto.getEdges() : new ArrayList<>();

        WorkflowGraphDto graph = new WorkflowGraphDto(nodes, edges);
        if (!nodes.isEmpty()) {
            validationService.validateWorkflowGraph(graph);
        }

        String definitionJson = serializeGraph(graph);
        Workflow workflow = new Workflow(exportDto.getName(), exportDto.getDescription(), definitionJson);
        workflow.setStatus(WorkflowStatus.DRAFT);
        workflow.setUserId(SecurityUtils.getCurrentUserId());

        Workflow saved = workflowRepository.save(workflow);
        return toWorkflowDetailResponse(saved, nodes, edges);
    }

    private void checkOwnership(Workflow workflow, String action) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        // Workflows with no owner (userId == null) are system/shared templates accessible to all users
        if (workflow.getUserId() == null || isAdmin) {
            return;
        }

        if (currentUserId == null || !workflow.getUserId().equals(currentUserId)) {
            log.warn("IDOR Defense: Unauthorized attempt by user [{}] to {} workflow [{}] owned by [{}]",
                    currentUserId, action, workflow.getId(), workflow.getUserId());
            throw new AccessDeniedException("You do not have permission to " + action + " this workflow");
        }
    }

    private String calculateSha256(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
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
