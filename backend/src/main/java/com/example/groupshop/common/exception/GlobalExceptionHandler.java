package com.example.groupshop.common.exception;

import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler that converts exceptions into {@link ApiResponse} error format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Parameter validation ───────────────────────────────────────

    /**
     * Handle {@code @Valid} annotated request body validation failures.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid value",
                        (a, b) -> b
                ));
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    /**
     * Handle {@code @Validated} path/param validation failures.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (a, b) -> b
                ));
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, violations);
    }

    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        Map<String, String> details = new HashMap<>();
        details.put(ex.getParameterName(), "参数不能为空");
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, details);
    }

    /**
     * Handle type mismatch for path variables or request parameters.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> details = new HashMap<>();
        details.put(ex.getName(), "参数类型不合法");
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, details);
    }

    /**
     * Handle unreadable request body (e.g. malformed JSON).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, "请求体格式错误");
    }

    // ── Routing errors ─────────────────────────────────────────────

    /**
     * Handle unknown routes (404).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoHandler(NoHandlerFoundException ex) {
        return buildErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, null);
    }

    /**
     * Handle HTTP method not allowed (405).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return buildErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, "请求方法不被支持");
    }

    // ── Business exception ─────────────────────────────────────────

    /**
     * Handle business exceptions with their specific error codes and HTTP status.
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex, HttpServletResponse response) {
        response.setStatus(ex.getHttpStatus());
        return buildErrorResponse(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), ex.getDetails());
    }

    // ── Default ────────────────────────────────────────────────────

    /**
     * Fallback handler for any unhandled exception.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error processing request: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildErrorResponse(ErrorCode.INTERNAL_ERROR, null);
    }

    // ── Helpers ────────────────────────────────────────────────────

    private ApiResponse<Void> buildErrorResponse(ErrorCode errorCode, Object details) {
        return buildErrorResponse(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getDefaultMessage(), details);
    }

    private ApiResponse<Void> buildErrorResponse(ErrorCode errorCode, String customMessage) {
        return buildErrorResponse(errorCode.getHttpStatus(), errorCode.getCode(), customMessage, null);
    }

    private ApiResponse<Void> buildErrorResponse(int httpStatus, String code, String message, Object details) {
        ApiResponse<Void> response = ApiResponse.error(code, message, details);
        response.setTraceId(MDC.get("traceId"));
        return response;
    }
}
