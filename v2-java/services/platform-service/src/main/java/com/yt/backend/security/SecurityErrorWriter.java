package com.yt.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ghi body loi xac thuc { code, error } (giu dinh dang cu, authFetch / captchaGate cua frontend doc `code`) */
@Component
public class SecurityErrorWriter {

    private final ObjectMapper mapper;

    public SecurityErrorWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletResponse res, AuthError error) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", error.name());
        body.put("error", error.message);
        res.setStatus(error.status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(res.getOutputStream(), body);
    }
}
