package com.marketflow.exception;

public class WorkflowNotFoundException extends RuntimeException {

    private final String workflowId;

    public WorkflowNotFoundException(String workflowId) {
        super("Workflow does not exist.");
        this.workflowId = workflowId;
    }

    public String getWorkflowId() {
        return workflowId;
    }
}
