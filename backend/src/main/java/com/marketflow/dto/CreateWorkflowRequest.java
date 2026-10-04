package com.marketflow.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class CreateWorkflowRequest {

    @NotBlank(message = "Workflow name is required")
    private String name;

    private String description;
    private List<NodeDto> nodes = new ArrayList<>();
    private List<EdgeDto> edges = new ArrayList<>();

    public CreateWorkflowRequest() {}

    public CreateWorkflowRequest(String name, String description, List<NodeDto> nodes, List<EdgeDto> edges) {
        this.name = name;
        this.description = description;
        if (nodes != null) this.nodes = nodes;
        if (edges != null) this.edges = edges;
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
