package com.marketflow.exception;

public class InvalidWorkflowGraphException extends RuntimeException {

    private final String errorCode;

    public InvalidWorkflowGraphException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
