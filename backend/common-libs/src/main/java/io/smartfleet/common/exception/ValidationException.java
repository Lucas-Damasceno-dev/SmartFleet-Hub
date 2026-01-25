package io.smartfleet.common.exception;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends SmartFleetException {

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }
}
