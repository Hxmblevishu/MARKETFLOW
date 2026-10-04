package com.marketflow.exception;

public class DuplicateResourceException extends RuntimeException {

    private final String resourceName;
    private final String identifier;

    public DuplicateResourceException(String resourceName, String identifier) {
        super(resourceName + " with identifier [" + identifier + "] already exists.");
        this.resourceName = resourceName;
        this.identifier = identifier;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getIdentifier() {
        return identifier;
    }
}
