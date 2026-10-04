package com.marketflow.exception;

public class TemplateNotFoundException extends RuntimeException {

    private final String templateId;

    public TemplateNotFoundException(String templateId) {
        super("Workflow template [" + templateId + "] does not exist.");
        this.templateId = templateId;
    }

    public String getTemplateId() {
        return templateId;
    }
}
