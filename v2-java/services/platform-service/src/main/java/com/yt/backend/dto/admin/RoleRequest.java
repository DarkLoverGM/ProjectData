package com.yt.backend.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RoleRequest(
        @NotNull(message = "Role không hợp lệ")
        @Pattern(regexp = "user|admin", message = "Role không hợp lệ") String role) {}
