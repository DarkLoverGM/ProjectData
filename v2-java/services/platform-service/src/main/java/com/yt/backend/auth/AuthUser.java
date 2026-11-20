package com.yt.backend.auth;

/** Payload cua access token (req.user ben Node). */
public record AuthUser(String id, String email, String username, String role) {
}
