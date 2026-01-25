package io.smartfleet.common.exception;

/**
 * Base exception for SmartFleet business errors.
 */
public class SmartFleetException extends RuntimeException {

    private final String errorCode;

    public SmartFleetException(String message) {
        super(message);
        this.errorCode = "SMARTFLEET_ERROR";
    }

    public SmartFleetException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SmartFleetException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SMARTFLEET_ERROR";
    }

    public SmartFleetException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
