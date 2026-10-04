package com.marketflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class WorkflowListResponse {

    @JsonProperty("workflows")
    private List<WorkflowSummaryDto> workflows = new ArrayList<>();

    public WorkflowListResponse() {}

    public WorkflowListResponse(List<WorkflowSummaryDto> workflows) {
        if (workflows != null) {
            this.workflows = workflows;
        }
    }

    public List<WorkflowSummaryDto> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<WorkflowSummaryDto> workflows) {
        this.workflows = workflows != null ? workflows : new ArrayList<>();
    }
}
