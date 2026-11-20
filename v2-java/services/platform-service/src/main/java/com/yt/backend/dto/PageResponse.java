package com.yt.backend.dto;

import java.util.List;

/** 1 trang du lieu: { items, total, page, limit } (trang admin dung chung) */
public record PageResponse<T>(List<T> items, long total, int page, int limit) {

    public static <T> PageResponse<T> of(List<T> items, long total, Paging paging) {
        return new PageResponse<>(items, total, paging.page(), paging.size());
    }
}
