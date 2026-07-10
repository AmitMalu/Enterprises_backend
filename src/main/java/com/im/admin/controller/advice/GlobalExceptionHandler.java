package com.im.admin.controller.advice;

import com.im.admin.dto.CustomResponseDTO;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.exception.FileValidationException;
import com.im.admin.exception.NoPendingRecordsFoundException;
import com.im.admin.exception.UserNotFoundException;
import com.im.admin.response.CommonResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Handle SpringDoc/Swagger specific exceptions - just log and return
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handleNoHandlerFound(NoHandlerFoundException ex, WebRequest request) {
        // Don't interfere with Swagger endpoints
        String path = request.getDescription(false);
        if (path.contains("swagger") || path.contains("api-docs")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Endpoint not found");
        response.put("path", ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(CustomValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(CustomValidationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("messages", ex.getErrors().stream()
                .map(err -> err.getField() + ": " + err.getMessage())
                .collect(Collectors.toList())
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<CustomResponseDTO> handleUserNotFound(UserNotFoundException ex) {
        log.error("UserNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CustomResponseDTO(400, ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CustomResponseDTO> handleRuntimeException(RuntimeException ex) {
        // Don't log Swagger-related exceptions as errors
        if (ex.getMessage() != null && (ex.getMessage().contains("swagger") || ex.getMessage().contains("api-docs"))) {
            log.debug("Swagger related exception: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CustomResponseDTO(500, "Internal server error"));
        }
        log.error("RuntimeException: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CustomResponseDTO(400, ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Collections.singletonMap("error", ex.getReason()));
    }

    @ExceptionHandler(NoPendingRecordsFoundException.class)
    public ResponseEntity<CommonResponse<String>> handleNoPendingRecords(NoPendingRecordsFoundException ex) {
        return ResponseEntity.ok(new CommonResponse<>(404, ex.getMessage(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<String> handleFileValidationException(FileValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CommonResponse<Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String message = "Duplicate entry or constraint violation";

        if (ex.getRootCause() != null && ex.getRootCause().getMessage() != null) {
            if (ex.getRootCause().getMessage().contains("abbreviation")) {
                message = "Abbreviation must be unique. This value already exists.";
            }
        }

        CommonResponse<Object> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
        response.setMessage(message);
        response.setData(null);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(
                CommonResponse.handleErrors(errorMessage)
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<Object>> handleConstraintViolations(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(
                CommonResponse.handleErrors(errorMessage)
        );
    }

    // This is the most important - handle general Exception but don't break Swagger
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Object>> handleGeneralException(Exception ex, WebRequest request) {
        String path = request.getDescription(false);

        // Log the FULL stack trace for debugging
        log.error("Full exception details: ", ex); // This will print the real erroR

        // If it's a Swagger/OpenAPI request, provide a more helpful response
        if (path.contains("v3/api-docs") || path.contains("swagger")) {
            log.error("Swagger/OpenAPI error: {}", ex.getMessage());
            CommonResponse<Object> response = new CommonResponse<>();
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Swagger documentation error. Please check configuration.");
            response.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // For non-Swagger requests, log the full error
        log.error("Something Went Wrong: {}", ex.getMessage(), ex);
        CommonResponse<Object> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMessage("Something Went Wrong - " + ex.getMessage());
        response.setData(null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<Map<String, Object>>
    buildErrorResponse(
            HttpStatus status,
            String message
    ) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("status", "FAILED");
        response.put("message", message);
        response.put("data", null);

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>>
    handleIllegalStateException(
            IllegalStateException exception
    ) {

        log.error(
                "Hierarchy data state error: {}",
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>>
    handleEntityNotFoundException(
            EntityNotFoundException exception
    ) {

        log.warn(
                "Hierarchy record not found: {}",
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>>
    handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {

        log.warn(
                "Invalid hierarchy API request: {}",
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }
}