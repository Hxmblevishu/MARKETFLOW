package com.marketflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowActionResponse {
    private String id;
    private String message;

    public WorkflowActionResponse() {}

    public WorkflowActionResponse(String message) {
        this.message = message;
    }

    public WorkflowActionResponse(String id, String message) {
        this.id = id;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
