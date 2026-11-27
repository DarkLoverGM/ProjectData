package com.yt.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VideoRequest(
        @NotBlank(message = "Tiêu đề video không được để trống")
        @Size(max = 200, message = "Tiêu đề video tối đa 200 ký tự")
        String title,

        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
        String description,

        @Size(max = 500, message = "URL thumbnail tối đa 500 ký tự")
        String thumbnail,

        @Size(max = 500, message = "URL video tối đa 500 ký tự")
        String videoUrl,

        @NotBlank(message = "Mã kênh (channelId) không được để trống")
        String channelId,

        @NotBlank(message = "Thể loại không được để trống")
        @Size(max = 50, message = "Thể loại tối đa 50 ký tự")
        String category,

        Boolean membersOnly,

        @Pattern(regexp = "PUBLIC|UNLISTED|PRIVATE", message = "Trạng thái phải là PUBLIC, UNLISTED hoặc PRIVATE")
        String status
) {}
