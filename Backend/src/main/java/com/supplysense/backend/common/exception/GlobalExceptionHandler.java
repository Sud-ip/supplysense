package com.supplysense.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.UncheckedIOException;

/**
 * M6 pass: every exception type that any module can realistically throw
 * now has an explicit mapping. Before this pass, IllegalArgumentException
 * (thrown across catalog, inventory, sales, forecasting for input
 * validation) and AccessDeniedException (thrown by @PreAuthorize
 * failures at the service layer) both fell through to the generic
 * 500 handler - a client-side validation problem or a permissions
 * failure was indistinguishable from a real server bug. That's fixed
 * here.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "DUPLICATE_EMAIL", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "INVALID_CREDENTIALS", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "INVALID_TOKEN", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "RESOURCE_NOT_FOUND", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateSkuException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSku(DuplicateSkuException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "DUPLICATE_SKU", ex.getMessage(), request.getRequestURI()));
    }

    // --- New in M6: these previously fell through to the generic 500 handler below ---

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        // Thrown by @PreAuthorize failures at the service layer (e.g.
        // STAFF calling an OWNER/MANAGER-only endpoint). Distinct from
        // the 401 an unauthenticated request gets - this user IS
        // authenticated, just not permitted to do this specific action.
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "ACCESS_DENIED", "You do not have permission to perform this action", request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        // Thrown across every module for programmatic input validation
        // that isn't expressible as a Bean Validation annotation (e.g.
        // "quantityDelta must not be zero", CSV row parsing failures).
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "INVALID_INPUT", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        // E.g. a malformed UUID in a path variable like /products/not-a-uuid
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "INVALID_PARAMETER", message, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "MALFORMED_REQUEST_BODY", "Request body is missing or not valid JSON", request.getRequestURI()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "FILE_TOO_LARGE", "Uploaded file exceeds the maximum allowed size", request.getRequestURI()));
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<ErrorResponse> handleFileReadError(UncheckedIOException ex, HttpServletRequest request) {
        // Thrown by SalesCsvImportService when the uploaded file can't be
        // read at all (not a row-level problem - those are captured per-row
        // in the import batch's error list, not thrown as exceptions).
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "FILE_READ_ERROR", "Could not read the uploaded file", request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "VALIDATION_ERROR", message, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        // By the time we get here, it's genuinely unexpected - everything
        // anticipated has its own handler above. Log the full stack trace
        // (structured logging picks this up with a stack_trace field) so
        // it's investigable, but never leak internal details to the client.
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "INTERNAL_ERROR", "An unexpected error occurred", request.getRequestURI()));
    }
}
