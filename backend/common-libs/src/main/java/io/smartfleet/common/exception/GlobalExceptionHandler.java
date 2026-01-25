package io.smartfleet.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler implementing RFC 7807 ProblemDetails.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://smartfleet.io/errors/resource-not-found"));
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        problemDetail.setProperty("timestamp", OffsetDateTime.now());
        
        return problemDetail;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRuleViolation(BusinessRuleException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Business Rule Violation");
        problemDetail.setType(URI.create("https://smartfleet.io/errors/business-rule-violation"));
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        problemDetail.setProperty("timestamp", OffsetDateTime.now());
        
        return problemDetail;
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("https://smartfleet.io/errors/validation-error"));
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        problemDetail.setProperty("timestamp", OffsetDateTime.now());
        
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation failed for request body");
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
        problemDetail.setTitle("Validation Failed");
        problemDetail.setType(URI.create("https://smartfleet.io/errors/validation-failed"));
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("timestamp", OffsetDateTime.now());
        
        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Constraint validation failed");
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setType(URI.create("https://smartfleet.io/errors/constraint-violation"));
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("timestamp", OffsetDateTime.now());
        
        return problemDetail;
    }

    @ExceptionHandler(SmartFleetException.class)
    public ProblemDetail handleSmartFleetException(SmartFleetException ex) {
        log.error("SmartFleet error: {}", ex.getMessage(), ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle("Application Error");
        problemDetail.setType(URI.create("https://smartfleet.io/errors/application-error"));
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        problemDetail.setProperty("timestamp", OffsetDateTime.now());
        
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://smartfleet.io/errors/internal-error"));
        problemDetail.setProperty("timestamp", OffsetDateTime.now());
        
        return problemDetail;
    }
}
