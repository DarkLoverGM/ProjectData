package com.yt.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
        String username,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Tên kênh không được để trống")
        @Size(max = 100, message = "Tên kênh tối đa 100 ký tự")
        String channelName,

        @Size(max = 300, message = "URL ảnh đại diện tối đa 300 ký tự")
        String avatar,

        @Size(max = 500, message = "Giới thiệu tối đa 500 ký tự")
        String bio,

        @NotBlank(message = "Vai trò không được để trống")
        @Pattern(regexp = "USER|CREATOR|ADMIN", message = "Vai trò phải là USER, CREATOR hoặc ADMIN")
        String role
) {}
