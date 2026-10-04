package com.marketflow.engine;

import com.marketflow.model.enums.ExecutionStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutionContext {

    private final String executionId;
    private final String workflowId;
    private final Map<String, Object> triggerPayload;
    private final Map<String, Object> nodeOutputs = new ConcurrentHashMap<>();
    private final Map<String, Object> variables = new ConcurrentHashMap<>();

    private ExecutionStatus status = ExecutionStatus.RUNNING;
    private String errorMessage;
    private final Instant startedAt;
    private Instant completedAt;

    public ExecutionContext(String executionId, String workflowId, Map<String, Object> triggerPayload) {
        this.executionId = executionId;
        this.workflowId = workflowId;
        this.triggerPayload = triggerPayload != null ? new ConcurrentHashMap<>(triggerPayload) : new ConcurrentHashMap<>();
        this.startedAt = Instant.now();
        // Seed variables with trigger payload under "trigger" namespace
        this.variables.put("trigger", this.triggerPayload);
    }

    public void setNodeOutput(String nodeId, Object output) {
        if (output != null) {
            nodeOutputs.put(nodeId, output);
            variables.put(nodeId, output);
        }
    }

    public Object getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }

    public Object getVariable(String key) {
        if (key == null) return null;
        if (variables.containsKey(key)) {
            return variables.get(key);
        }
        // Check nested path, e.g. "trigger.email"
        if (key.contains(".")) {
            String[] parts = key.split("\\.", 2);
            Object root = variables.get(parts[0]);
            if (root instanceof Map<?, ?> map) {
                return map.get(parts[1]);
            }
        }
        return null;
    }

    public void setVariable(String key, Object value) {
        if (key != null && value != null) {
            variables.put(key, value);
        }
    }

    // Getters and Setters
    public String getExecutionId() {
        return executionId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public Map<String, Object> getTriggerPayload() {
        return Collections.unmodifiableMap(triggerPayload);
    }

    public Map<String, Object> getNodeOutputs() {
        return Collections.unmodifiableMap(nodeOutputs);
    }

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
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

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
