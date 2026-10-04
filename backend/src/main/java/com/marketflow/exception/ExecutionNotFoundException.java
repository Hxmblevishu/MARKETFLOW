package com.marketflow.exception;

public class ExecutionNotFoundException extends RuntimeException {

    private final String executionId;

    public ExecutionNotFoundException(String executionId) {
        super("Execution does not exist.");
        this.executionId = executionId;
    }

    public String getExecutionId() {
        return executionId;
    }
}
