package com.yt.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Ket noi /ws/user cua nguoi dung da dang nhap (frontend: src/utils/userRealtime.js).
 * Su kien chi gui toi dung chu cua no, khac voi phong video:<id> cong khai (Realtime).
 */
@Component
public class UserRealtime extends AuthSockets {

    public UserRealtime(ObjectMapper mapper) {
        super(mapper, "user");
    }

    public void sendTo(String userId, UserEvent event) {
        if (userId != null) sendWhere(event, userId);
    }
}
