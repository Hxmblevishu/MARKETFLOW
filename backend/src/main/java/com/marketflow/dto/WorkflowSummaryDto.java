package com.marketflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marketflow.model.enums.WorkflowStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowSummaryDto {
    private String id;
    private String name;
    private WorkflowStatus status;
    private Instant updatedAt;

    public WorkflowSummaryDto() {}

    public WorkflowSummaryDto(String id, String name, WorkflowStatus status, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
