package com.marketflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marketflow.model.enums.WorkflowStatus;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowDetailResponse {
    private String id;
    private String name;
    private String description;
    private WorkflowStatus status;
    private List<NodeDto> nodes = new ArrayList<>();
    private List<EdgeDto> edges = new ArrayList<>();

    public WorkflowDetailResponse() {}

    public WorkflowDetailResponse(String id, String name, String description, WorkflowStatus status, List<NodeDto> nodes, List<EdgeDto> edges) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        if (nodes != null) this.nodes = nodes;
        if (edges != null) this.edges = edges;
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
