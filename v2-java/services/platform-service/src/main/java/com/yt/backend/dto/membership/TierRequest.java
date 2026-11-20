package com.yt.backend.dto.membership;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Tao goi hoi vien. Gia tinh theo thang, VND. */
public record TierRequest(
        @NotBlank(message = "Tên gói không được để trống")
        @Size(max = 50, message = "Tên gói tối đa 50 ký tự") String name,
        @NotNull(message = "Thiếu giá gói")
        @Min(value = 10_000, message = "Giá mỗi tháng từ 10.000đ đến 2.000.000đ")
        @Max(value = 2_000_000, message = "Giá mỗi tháng từ 10.000đ đến 2.000.000đ") Long price,
        @Size(max = 500, message = "Quyền lợi tối đa 500 ký tự") String perks) {

    @AssertTrue(message = "Giá phải là bội số của 1.000đ")
    public boolean isPriceRounded() {
        return price == null || price % 1000 == 0;
    }
}
