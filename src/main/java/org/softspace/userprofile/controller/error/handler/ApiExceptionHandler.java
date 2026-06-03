package org.softspace.userprofile.controller.error.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.softspace.userprofile.controller.error.ErrorCode;
import org.softspace.userprofile.exception.EmailAlreadyExistsException;
import org.softspace.userprofile.exception.UserProfileNotFoundException;
import org.softspace.userprofile.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.softspace.userprofile.dto.error.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private static final String FIELD_NAME = "fieldName";

    private final Tracer tracer;

    // TODO: Нужно ли в поле path передовать Http-request вместе с query-параметрами.   === DONE ===

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> userProfileNotFoundExceptionHandle(UserProfileNotFoundException ex, HttpServletRequest request) {
        String message = StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "User profile not found.";
        log.warn(message, ex);
        Instant now = Instant.now();
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getErrorCode().name(),
                message,
                ex.getDetails(),
                request.getRequestURI(),
                now,
                getCurrentTraceId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }


    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> emailAlreadyExistsExceptionHandle(EmailAlreadyExistsException ex, HttpServletRequest request) {
        String message = StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Email already exists.";
        log.warn(message, ex);
        Instant now = Instant.now();
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getErrorCode().name(),
                message,
                ex.getDetails(),
                request.getRequestURI(),
                now,
                getCurrentTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> notReadableExceptionHandle(HttpMessageNotReadableException ex, HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        switch(cause) {
            case InvalidFormatException exception -> {
                String message = StringUtils.isNotBlank(exception.getMessage()) ? exception.getMessage() : "invalid format of field.";
                log.warn(message, ex);
                Instant now = Instant.now();
                ErrorResponse errorResponse = new ErrorResponse(
                        ErrorCode.FIELD_INVALID.name(),
                        message,
                        Map.of(FIELD_NAME, exception.getPath().getFirst().getFieldName(),
                                "value", exception.getValue()),
                        request.getRequestURI(),
                        now,
                        getCurrentTraceId()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            case UnrecognizedPropertyException exception -> {
                String message = StringUtils.isNotBlank(exception.getMessage()) ? exception.getMessage() : "unrecognized property of field.";
                log.warn(message, ex);
                Instant now = Instant.now();
                ErrorResponse errorResponse = new ErrorResponse(
                        ErrorCode.FIELD_NOT_ALLOWED.name(),
                        message,
                        Map.of(FIELD_NAME, exception.getPropertyName()),
                        request.getRequestURI(),
                        now,
                        getCurrentTraceId()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            default -> {
                String message = StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Invalid JSON.";
                log.warn(message, ex);
                Instant now = Instant.now();
                ErrorResponse errorResponse = new ErrorResponse(
                        ErrorCode.BAD_REQUEST.name(),
                        message,
                        Map.of(),
                        request.getRequestURI(),
                        now,
                        getCurrentTraceId()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> forbiddenFieldExceptionHandle(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Validation error.";
        log.warn(message, ex);
        Instant now = Instant.now();
        Map<String, Object> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.name(),
                message,
                details,
                request.getRequestURI(),
                now,
                getCurrentTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> validationBusinessExceptionHandle(ValidationException ex, HttpServletRequest request) {
        String message = StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "No valid data.";
        log.warn(message, ex);
        Instant now = Instant.now();
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getErrorCode().name(),
                message,
                ex.getDetails(),
                request.getRequestURI(),
                now,
                getCurrentTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> internalExceptionHandle(RuntimeException ex, HttpServletRequest request) {
        String message = StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "Unexpected error.";
        log.error(message, ex);
        Instant now = Instant.now();
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR.name(),
                message,
                Map.of(),
                request.getRequestURI(),
                now,
                getCurrentTraceId()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String getCurrentTraceId() {
        Span span = tracer.currentSpan();
        if (span == null) {
            return null;
        }
        return span.context().traceId();
    }
}
