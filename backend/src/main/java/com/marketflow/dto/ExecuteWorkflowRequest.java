package com.marketflow.dto;

import java.util.HashMap;
import java.util.Map;

public class ExecuteWorkflowRequest {
    private Map<String, Object> input = new HashMap<>();

    public ExecuteWorkflowRequest() {}

    public ExecuteWorkflowRequest(Map<String, Object> input) {
        if (input != null) {
            this.input = input;
        }
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input != null ? input : new HashMap<>();
    }
}
