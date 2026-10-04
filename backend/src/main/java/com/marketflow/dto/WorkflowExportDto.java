package com.marketflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Exported Workflow Definition Bundle")
public class WorkflowExportDto {

    @Schema(description = "Schema specification version", example = "1.0")
    private String schemaVersion = "1.0";

    @Schema(description = "ISO-8601 timestamp of export", example = "2026-10-04T12:00:00Z")
    private String exportedAt = Instant.now().toString();

    @NotBlank(message = "Workflow name is required for export/import")
    @Schema(description = "Name of the workflow", example = "Lead Scoring & Outreach")
    private String name;

    @Schema(description = "Description of workflow purpose", example = "Multi-step lead pipeline")
    private String description;

    @NotNull(message = "Nodes list cannot be null")
    @Schema(description = "Array of workflow graph nodes")
    private List<NodeDto> nodes = new ArrayList<>();

    @NotNull(message = "Edges list cannot be null")
    @Schema(description = "Array of workflow graph edges connecting nodes")
    private List<EdgeDto> edges = new ArrayList<>();

    @Schema(description = "Integrity checksum (SHA-256)", example = "a9b8c7d6e5...")
    private String checksum;

    public WorkflowExportDto() {}

    public WorkflowExportDto(String schemaVersion, String exportedAt, String name, String description,
                             List<NodeDto> nodes, List<EdgeDto> edges, String checksum) {
        this.schemaVersion = schemaVersion;
        this.exportedAt = exportedAt;
        this.name = name;
        this.description = description;
        this.nodes = nodes != null ? nodes : new ArrayList<>();
        this.edges = edges != null ? edges : new ArrayList<>();
        this.checksum = checksum;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(String exportedAt) {
        this.exportedAt = exportedAt;
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
        this.nodes = nodes;
    }

    public List<EdgeDto> getEdges() {
        return edges;
    }

    public void setEdges(List<EdgeDto> edges) {
        this.edges = edges;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
