package com.yt.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Ket noi /ws/admin cua trang quan tri: moi su kien gui cho tat ca admin dang mo trang */
@Component
public class AdminRealtime extends AuthSockets {

    public AdminRealtime(ObjectMapper mapper) {
        super(mapper, "admin");
    }

    public void publish(AdminEvent event) {
        sendWhere(event, null);
    }
}
