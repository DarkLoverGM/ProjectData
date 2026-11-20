package com.yt.backend.common;

import com.yt.backend.dto.ErrorResponse;

/**
 * Nem ra o bat ky dau (service, controller) de tra ngay status + body loi.
 * GlobalExceptionHandler bat va tra ErrorResponse, controller khong phai tu if/else tung loi.
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final transient ErrorResponse body;

    public ApiException(int status, ErrorResponse body) {
        super(body.message());
        this.status = status;
        this.body = body;
    }

    public ApiException(int status, String message) {
        this(status, ErrorResponse.of(message));
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(403, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, message);
    }

    public int getStatus() {
        return status;
    }

    public ErrorResponse getBody() {
        return body;
    }
}
