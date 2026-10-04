package com.marketflow.engine;

import com.marketflow.model.enums.StepStatus;

public class NodeExecutionResult {

    private final String nodeId;
    private final StepStatus status;
    private final Object outputData;
    private final String selectedHandle;
    private final String errorMessage;

    private NodeExecutionResult(String nodeId, StepStatus status, Object outputData, String selectedHandle, String errorMessage) {
        this.nodeId = nodeId;
        this.status = status;
        this.outputData = outputData;
        this.selectedHandle = selectedHandle != null ? selectedHandle : "default";
        this.errorMessage = errorMessage;
    }

    public static NodeExecutionResult success(String nodeId, Object outputData) {
        return new NodeExecutionResult(nodeId, StepStatus.COMPLETED, outputData, "default", null);
    }

    public static NodeExecutionResult conditional(String nodeId, Object outputData, String selectedHandle) {
        return new NodeExecutionResult(nodeId, StepStatus.COMPLETED, outputData, selectedHandle, null);
    }

    public static NodeExecutionResult failed(String nodeId, String errorMessage) {
        return new NodeExecutionResult(nodeId, StepStatus.FAILED, null, null, errorMessage);
    }

    public String getNodeId() {
        return nodeId;
    }

    public StepStatus getStatus() {
        return status;
    }

    public Object getOutputData() {
        return outputData;
    }

    public String getSelectedHandle() {
        return selectedHandle;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return status == StepStatus.COMPLETED;
    }
}
