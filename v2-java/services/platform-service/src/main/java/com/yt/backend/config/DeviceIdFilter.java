package com.yt.backend.config;

import com.yt.backend.security.AccessTokenReader;
import com.yt.backend.auth.DeviceIds;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Gan device id hop le vao request (app.use(deviceId) ben Node), chay truoc rate limit */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class DeviceIdFilter extends OncePerRequestFilter {

    private final DeviceIds deviceIds;

    public DeviceIdFilter(DeviceIds deviceIds) {
        this.deviceIds = deviceIds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String fromCookie = deviceIds.verify(AccessTokenReader.readCookie(req, DeviceIds.DEVICE_COOKIE));
        String fromHeader = fromCookie != null ? null : deviceIds.verify(req.getHeader(DeviceIds.DEVICE_HEADER));
        req.setAttribute(DeviceIds.ATTR, fromCookie != null ? fromCookie : fromHeader);
        // Cookie bi xoa nhung localStorage con -> ghi lai cookie
        if (fromHeader != null) deviceIds.setCookie(res, fromHeader);
        chain.doFilter(req, res);
    }
}
