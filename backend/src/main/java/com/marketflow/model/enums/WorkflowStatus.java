package com.marketflow.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkflowStatus {
    DRAFT("draft"),
    ACTIVE("active"),
    PAUSED("paused");

    private final String value;

    WorkflowStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static WorkflowStatus fromValue(String value) {
        if (value == null) return DRAFT;
        for (WorkflowStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return DRAFT;
    }
}
