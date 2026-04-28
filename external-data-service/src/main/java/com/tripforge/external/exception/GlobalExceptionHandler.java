package com.tripforge.external.exception;

import com.tripforge.external.dto.ProviderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for external-data-service.
 * Returns structured ProviderResponse error format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProviderResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
                errors.put(((FieldError) e).getField(), e.getDefaultMessage()));
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(ProviderResponse.<Map<String, String>>builder()
                        .data(errors)
                        .sourceProvider("none")
                        .degradedMode(true)
                        .warnings(List.of("Request validation failed: " + errors))
                        .build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProviderResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(ProviderResponse.<Void>builder()
                        .sourceProvider("none")
                        .degradedMode(true)
                        .warnings(List.of("Missing required parameter: " + ex.getParameterName()))
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProviderResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error in external-data-service: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProviderResponse.<Void>builder()
                        .sourceProvider("none")
                        .degradedMode(true)
                        .warnings(List.of("Internal error: " + ex.getMessage()))
                        .build());
    }
}
