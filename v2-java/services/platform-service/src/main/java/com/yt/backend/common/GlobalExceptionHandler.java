package com.yt.backend.common;

import com.yt.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Doi moi loi thanh JSON { success: false, message } de controller khong phai tu bat loi.
 * - ApiException: loi nghiep vu service nem ra (sai mat khau, khong tim thay...)
 * - Loi Bean Validation (@Valid tren DTO): tra 400 kem thong bao cua rang buoc dau tien
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getBody());
    }

    /** @Valid tren @RequestBody / @ModelAttribute khong qua */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> invalid(BindException e) {
        // Loi field (vd. thieu email) uu tien hon loi cap object (vd. @AssertTrue)
        ObjectError error = e.getFieldErrors().stream().map(f -> (ObjectError) f).findFirst()
                .orElse(e.getGlobalErrors().stream().findFirst().orElse(null));
        String message = error == null ? "Dữ liệu không hợp lệ" : error.getDefaultMessage();
        if (error instanceof FieldError f && f.isBindingFailure()) message = "Dữ liệu \"" + f.getField() + "\" không hợp lệ";
        return ResponseEntity.badRequest().body(ErrorResponse.of(message));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> invalidParams(HandlerMethodValidationException e) {
        String message = e.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(err -> err.getDefaultMessage()).findFirst().orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(ErrorResponse.of(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> invalidConstraint(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream().map(v -> v.getMessage()).findFirst().orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(ErrorResponse.of(message));
    }

    /** Body khong phai JSON hop le / sai kieu (vd. gui chu cho field so) */
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> unreadable() {
        return ResponseEntity.badRequest().body(ErrorResponse.of("Dữ liệu gửi lên không hợp lệ"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> tooLarge() {
        return ResponseEntity.status(413).body(ErrorResponse.of("File quá lớn (video dài tối đa 2GB)"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> notFound(HttpServletRequest req) {
        return ResponseEntity.status(404).body("Cannot " + req.getMethod() + " " + req.getRequestURI());
    }

    /**
     * Loi phan quyen (@PreAuthorize) phai de Spring Security xu ly (tra 401 / 403 dung ma),
     * khong duoc roi vao handler Exception ben duoi.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void security(RuntimeException e) {
        throw e;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> other(Exception e, HttpServletRequest req) {
        log.error("{} {} error", req.getMethod(), req.getRequestURI(), e);
        return ResponseEntity.status(500).body(ErrorResponse.of("Lỗi server"));
    }
}
