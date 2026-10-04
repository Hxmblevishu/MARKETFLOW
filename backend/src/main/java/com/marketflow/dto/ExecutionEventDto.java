package com.marketflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Real-time execution event broadcast payload")
public class ExecutionEventDto {

    private String executionId;
    private String workflowId;
    private String nodeId;
    private String nodeLabel;
    private String eventType;
    private String status;
    private Long durationMs;
    private Instant timestamp;
    private Object data;
    private String error;

    public ExecutionEventDto() {
        this.timestamp = Instant.now();
    }

    public ExecutionEventDto(String executionId, String workflowId, String nodeId, String nodeLabel,
                             String eventType, String status, Long durationMs, Object data, String error) {
        this.executionId = executionId;
        this.workflowId = workflowId;
        this.nodeId = nodeId;
        this.nodeLabel = nodeLabel;
        this.eventType = eventType;
        this.status = status;
        this.durationMs = durationMs;
        this.timestamp = Instant.now();
        this.data = data;
        this.error = error;
    }

    public static ExecutionEventDto executionStarted(String executionId, String workflowId) {
        return new ExecutionEventDto(executionId, workflowId, null, null, "EXECUTION_STARTED", "RUNNING", null, null, null);
    }

    public static ExecutionEventDto nodeStarted(String executionId, String workflowId, String nodeId, String nodeLabel) {
        return new ExecutionEventDto(executionId, workflowId, nodeId, nodeLabel, "NODE_STARTED", "RUNNING", null, null, null);
    }

    public static ExecutionEventDto nodeCompleted(String executionId, String workflowId, String nodeId, String nodeLabel,
                                                  String status, Long durationMs, Object data) {
        return new ExecutionEventDto(executionId, workflowId, nodeId, nodeLabel, "NODE_COMPLETED", status, durationMs, data, null);
    }

    public static ExecutionEventDto nodeFailed(String executionId, String workflowId, String nodeId, String nodeLabel,
                                               Long durationMs, String error) {
        return new ExecutionEventDto(executionId, workflowId, nodeId, nodeLabel, "NODE_FAILED", "FAILED", durationMs, null, error);
    }

    public static ExecutionEventDto executionFinished(String executionId, String workflowId, String status, String error) {
        return new ExecutionEventDto(executionId, workflowId, null, null, "EXECUTION_FINISHED", status, null, null, error);
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

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeLabel() {
        return nodeLabel;
    }

    public void setNodeLabel(String nodeLabel) {
        this.nodeLabel = nodeLabel;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
