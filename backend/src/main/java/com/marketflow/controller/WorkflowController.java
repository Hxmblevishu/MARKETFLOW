package com.marketflow.controller;

import com.marketflow.dto.*;
import com.marketflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/workflows")
@Tag(name = "Workflows", description = "Workflow Management & Graph Operations")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @Operation(summary = "Create workflow", description = "Creates and validates a new marketing workflow graph")
    public ResponseEntity<WorkflowDetailResponse> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        WorkflowDetailResponse response = workflowService.createWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List workflows", description = "Retrieves all workflows ordered by last updated")
    public ResponseEntity<WorkflowListResponse> listWorkflows() {
        return ResponseEntity.ok(workflowService.listWorkflows());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow by ID", description = "Returns full workflow specification including nodes and edges")
    public ResponseEntity<WorkflowDetailResponse> getWorkflow(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update workflow", description = "Updates workflow metadata and graph structure")
    public ResponseEntity<WorkflowDetailResponse> updateWorkflow(@PathVariable String id,
                                                                 @RequestBody UpdateWorkflowRequest request) {
        return ResponseEntity.ok(workflowService.updateWorkflow(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete workflow", description = "Permanently deletes a workflow and associated executions")
    public ResponseEntity<WorkflowActionResponse> deleteWorkflow(@PathVariable String id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.ok(new WorkflowActionResponse(id, "Workflow successfully deleted"));
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Duplicate workflow", description = "Clones an existing workflow definition as a new draft")
    public ResponseEntity<WorkflowDetailResponse> duplicateWorkflow(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.duplicateWorkflow(id));
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Export workflow", description = "Exports workflow definition bundle as portable JSON with integrity checksum")
    public ResponseEntity<WorkflowExportDto> exportWorkflow(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.exportWorkflow(id));
    }

    @PostMapping("/import")
    @Operation(summary = "Import workflow", description = "Imports a workflow definition bundle and recreates the DAG canvas")
    public ResponseEntity<WorkflowDetailResponse> importWorkflow(@Valid @RequestBody WorkflowExportDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.importWorkflow(request));
    }
}
