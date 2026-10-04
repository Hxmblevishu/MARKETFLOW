package com.marketflow.dto;

import jakarta.validation.constraints.NotBlank;

public class AiGenerateWorkflowRequest {

    @NotBlank(message = "Prompt cannot be empty")
    private String prompt;

    public AiGenerateWorkflowRequest() {}

    public AiGenerateWorkflowRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
