package com.marketflow.dto;

import com.marketflow.model.enums.WorkflowStatus;

import java.util.ArrayList;
import java.util.List;

public class UpdateWorkflowRequest {
    private String name;
    private String description;
    private WorkflowStatus status;
    private List<NodeDto> nodes = new ArrayList<>();
    private List<EdgeDto> edges = new ArrayList<>();

    public UpdateWorkflowRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public List<NodeDto> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeDto> nodes) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
    }

    public List<EdgeDto> getEdges() {
        return edges;
    }

    public void setEdges(List<EdgeDto> edges) {
        this.edges = edges != null ? edges : new ArrayList<>();
    }
}
