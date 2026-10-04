package com.marketflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowGraphDto {
    private List<NodeDto> nodes = new ArrayList<>();
    private List<EdgeDto> edges = new ArrayList<>();

    public WorkflowGraphDto() {}

    public WorkflowGraphDto(List<NodeDto> nodes, List<EdgeDto> edges) {
        if (nodes != null) this.nodes = nodes;
        if (edges != null) this.edges = edges;
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
