package com.yt.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Request chua dang nhap goi endpoint can dang nhap -> 401 NO_TOKEN (hoac 403 ACCOUNT_BANNED neu bi khoa) */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorWriter errors;

    public RestAuthenticationEntryPoint(SecurityErrorWriter errors) {
        this.errors = errors;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e) throws IOException {
        Object reason = req.getAttribute(AuthError.ATTR);
        errors.write(res, reason instanceof AuthError a ? a : AuthError.NO_TOKEN);
    }
}
