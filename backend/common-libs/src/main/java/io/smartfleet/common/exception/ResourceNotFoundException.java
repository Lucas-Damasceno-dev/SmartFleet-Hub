package io.smartfleet.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends SmartFleetException {

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(String.format("%s with identifier '%s' not found", resourceType, identifier), "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
}
