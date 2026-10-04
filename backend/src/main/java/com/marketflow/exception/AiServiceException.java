package com.marketflow.exception;

public class AiServiceException extends RuntimeException {

    private final String provider;

    public AiServiceException(String message) {
        this("AI_PROVIDER", message, null);
    }

    public AiServiceException(String provider, String message) {
        this(provider, message, null);
    }

    public AiServiceException(String provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
