package com.marketflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marketflow.model.enums.ExecutionStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionDetailResponse {
    private String id;
    private String workflowId;
    private ExecutionStatus status;
    private Object input;
    private Object output;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;
    private List<ExecutionStepDto> steps = new ArrayList<>();

    public ExecutionDetailResponse() {}

    public ExecutionDetailResponse(String id, String workflowId, ExecutionStatus status, Instant startedAt, Instant completedAt, List<ExecutionStepDto> steps) {
        this.id = id;
        this.workflowId = workflowId;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        if (steps != null) {
            this.steps = steps;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<ExecutionStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<ExecutionStepDto> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }
}
