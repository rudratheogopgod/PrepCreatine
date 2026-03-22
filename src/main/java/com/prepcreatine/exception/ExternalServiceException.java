package com.prepcreatine.exception;

public class ExternalServiceException extends RuntimeException {
    private final String serviceName;

    public ExternalServiceException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
    }

    public String getServiceName() { return serviceName; }
}
