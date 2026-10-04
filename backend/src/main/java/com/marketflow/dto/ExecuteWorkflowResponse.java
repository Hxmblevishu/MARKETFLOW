package com.marketflow.dto;

import com.marketflow.model.enums.ExecutionStatus;

public class ExecuteWorkflowResponse {
    private String executionId;
    private String workflowId;
    private ExecutionStatus status;

    public ExecuteWorkflowResponse() {}

    public ExecuteWorkflowResponse(String executionId, String workflowId, ExecutionStatus status) {
        this.executionId = executionId;
        this.workflowId = workflowId;
        this.status = status;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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
}
