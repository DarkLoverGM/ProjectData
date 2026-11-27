package com.yt.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message", "Dữ liệu không hợp lệ",
                        "errors", errors
                ));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ResourceAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Không thể kết nối đến Service phụ thuộc"));
    }

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    public ResponseEntity<Map<String, Object>> handleRemoteNotFound(HttpClientErrorException.NotFound ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Không tìm thấy dữ liệu tại Service phụ thuộc"));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleRemoteError(RestClientResponseException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "Service phụ thuộc trả về lỗi không mong đợi: " + ex.getStatusCode().value()));
    }
}
