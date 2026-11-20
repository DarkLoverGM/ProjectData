package com.yt.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Da dang nhap nhung khong du quyen (user thuong vao /api/admin) -> 403 FORBIDDEN */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter errors;

    public RestAccessDeniedHandler(SecurityErrorWriter errors) {
        this.errors = errors;
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e) throws IOException {
        errors.write(res, AuthError.FORBIDDEN);
    }
}
