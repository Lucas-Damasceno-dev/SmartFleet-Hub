package io.smartfleet.common.exception;

/**
 * Exception thrown when a business rule is violated.
 */
public class BusinessRuleException extends SmartFleetException {

    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessRuleException(String message, String errorCode) {
        super(message, errorCode);
    }
}
