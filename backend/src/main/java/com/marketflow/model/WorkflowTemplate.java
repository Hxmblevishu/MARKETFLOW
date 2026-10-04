package com.marketflow.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_templates", indexes = {
    @Index(name = "idx_templates_category", columnList = "category"),
    @Index(name = "idx_templates_name", columnList = "name")
})
public class WorkflowTemplate {

    @Id
    @Column(name = "id", length = 64, nullable = false, updatable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "definition", columnDefinition = "TEXT", nullable = false)
    private String definition = "{\"nodes\":[],\"edges\":[]}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WorkflowTemplate() {}

    public WorkflowTemplate(String name, String category, String description, String definition) {
        this.name = name;
        this.category = category;
        this.description = description;
        if (definition != null && !definition.isBlank()) {
            this.definition = definition;
        }
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = "tpl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Getters and Setters
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
