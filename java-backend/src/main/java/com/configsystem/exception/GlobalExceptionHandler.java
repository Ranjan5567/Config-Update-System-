package com.configsystem.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralised exception handler + all application exception types.
 *
 * Exception types are nested static classes here so the whole
 * error-handling story lives in one file.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Exception types ───────────────────────────────────────────────────────

    /** Thrown when a merchant ID is not present in the merchants table. → HTTP 404 */
    public static class MerchantNotFoundException extends RuntimeException {
        public MerchantNotFoundException(Long id) {
            super("Merchant " + id + " not found.");
        }
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(MerchantNotFoundException ex) {
        log.warn("Merchant not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Merchant Not Found",
                "not-found", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedJson(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Malformed JSON",
                "bad-json", "The provided JSON is malformed or invalid.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
        pd.setType(URI.create("https://configsystem/errors/validation"));
        pd.setTitle("Validation Error");
        pd.setProperty("fieldErrors", errors);
        pd.setProperty("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "internal", "An unexpected error occurred.");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title,
                                                   String errorKey, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("https://configsystem/errors/" + errorKey));
        pd.setTitle(title);
        pd.setProperty("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(pd);
    }
}
