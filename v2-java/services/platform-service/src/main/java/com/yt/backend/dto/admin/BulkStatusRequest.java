package com.yt.backend.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkStatusRequest(
        @NotEmpty(message = "Chưa chọn video nào")
        @Size(max = 200, message = "Tối đa 200 video mỗi lần") List<String> ids,
        @NotNull(message = "Status phải là 0 hoặc 1")
        @Min(value = 0, message = "Status phải là 0 hoặc 1")
        @Max(value = 1, message = "Status phải là 0 hoặc 1") Integer status) {}
