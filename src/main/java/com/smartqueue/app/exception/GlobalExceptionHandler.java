package com.smartqueue.app.exception;

import com.smartqueue.app.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiResponse<?> response = new ApiResponse<>(false, ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestException ex) {
        ApiResponse<?> response = new ApiResponse<>(false, ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Handle wrong username/password — show clean message
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials(BadCredentialsException ex) {
        ApiResponse<?> response = new ApiResponse<>(false, "Incorrect Username or Password", null);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // Handle all other Spring Security auth failures
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthException(AuthenticationException ex) {
        ApiResponse<?> response = new ApiResponse<>(false, "Incorrect Username or Password", null);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("DataIntegrityViolationException: {}", ex.getMostSpecificCause().getMessage());
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.toLowerCase().contains("unique")) {
            return new ResponseEntity<>(new ApiResponse<>(false, "A duplicate record was detected. Please try a different entry.", null), HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(new ApiResponse<>(false, "Database constraint violation: " + ex.getMostSpecificCause().getMessage(), null), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.dao.CannotAcquireLockException.class)
    public ResponseEntity<ApiResponse<?>> handleCannotAcquireLock(org.springframework.dao.CannotAcquireLockException ex) {
        log.error("Database lock acquisition failure / deadlock: {}", ex.getMessage());
        ApiResponse<?> response = new ApiResponse<>(false, "The booking system is currently busy. Please try booking your slot again in a moment.", null);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        // Don't leak internal error details to frontend
        ApiResponse<?> response = new ApiResponse<>(false, "Server error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
