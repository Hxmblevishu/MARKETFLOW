package com.marketflow.model;

import com.marketflow.model.enums.StepStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "execution_steps", indexes = {
    @Index(name = "idx_steps_execution_id", columnList = "execution_id"),
    @Index(name = "idx_steps_node_id", columnList = "node_id"),
    @Index(name = "idx_steps_status", columnList = "status"),
    @Index(name = "idx_steps_exec_started", columnList = "execution_id, started_at")
})
public class ExecutionStep {

    @Id
    @Column(name = "id", length = 64, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false, foreignKey = @ForeignKey(name = "fk_steps_execution"))
    private Execution execution;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @Column(name = "node_type", length = 64)
    private String nodeType;

    @Column(name = "step_name")
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private StepStatus status = StepStatus.PENDING;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData = "{}";

    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData = "{}";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public ExecutionStep() {}

    public ExecutionStep(Execution execution, String nodeId, String nodeType, String stepName) {
        this.execution = execution;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.stepName = stepName;
        this.status = StepStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = "step_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public StepStatus getStatus() {
        return status;
    }

    public void setStatus(StepStatus status) {
        this.status = status;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getOutputData() {
        return outputData;
    }

    public void setOutputData(String outputData) {
        this.outputData = outputData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
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
}
