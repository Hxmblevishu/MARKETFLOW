package com.marketflow.controller;

import com.marketflow.dto.ExecuteWorkflowRequest;
import com.marketflow.dto.ExecuteWorkflowResponse;
import com.marketflow.dto.ExecutionDetailResponse;
import com.marketflow.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Executions", description = "Workflow Execution Engine, Monitoring & Diagnostics")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/workflows/{workflowId}/execute")
    @Operation(summary = "Execute workflow", description = "Triggers workflow execution synchronously or asynchronously")
    public ResponseEntity<ExecuteWorkflowResponse> executeWorkflow(
            @PathVariable String workflowId,
            @RequestBody(required = false) ExecuteWorkflowRequest request,
            @RequestParam(name = "async", defaultValue = "false") boolean async) {

        ExecuteWorkflowRequest req = request != null ? request : new ExecuteWorkflowRequest();
        ExecuteWorkflowResponse response = async
                ? executionService.executeWorkflowAsync(workflowId, req)
                : executionService.executeWorkflow(workflowId, req);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/executions/{id}")
    @Operation(summary = "Get execution details", description = "Retrieves execution status, inputs/outputs, and step logs")
    public ResponseEntity<ExecutionDetailResponse> getExecution(@PathVariable String id) {
        return ResponseEntity.ok(executionService.getExecution(id));
    }

    @GetMapping("/executions")
    @Operation(summary = "List executions", description = "Lists recent executions, optionally filtered by workflowId")
    public ResponseEntity<List<ExecutionDetailResponse>> listExecutions(
            @RequestParam(name = "workflowId", required = false) String workflowId) {
        return ResponseEntity.ok(executionService.listExecutions(workflowId));
    }

    @PostMapping("/executions/{id}/retry")
    @Operation(summary = "Retry execution", description = "Re-runs a previous execution with identical trigger input")
    public ResponseEntity<ExecuteWorkflowResponse> retryExecution(@PathVariable String id) {
        return ResponseEntity.ok(executionService.retryExecution(id));
    }
}
