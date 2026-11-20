package com.yt.backend.dto;

/** Body thanh cong chi co thong bao: { success: true, message } */
public record MessageResponse(boolean success, String message) {

    public static MessageResponse ok(String message) {
        return new MessageResponse(true, message);
    }
}
