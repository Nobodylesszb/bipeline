package com.pipeline.platform.shared.api;

import java.util.Map;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                exception.errorCode().httpStatus().value(),
                exception.getMessage(),
                exception.details(),
                traceId(request)
        );
        return ResponseEntity.status(exception.errorCode().httpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                ErrorCode.VALIDATION_FAILED.httpStatus().value(),
                "Request validation failed",
                Map.of("fieldErrors", exception.getBindingResult().getFieldErrors().size()),
                traceId(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR.httpStatus().value(),
                "Internal server error",
                Map.of(),
                traceId(request)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute("traceId");
        if (traceId instanceof String value && !value.isBlank()) {
            return value;
        }
        return request.getRequestId();
    }
}
