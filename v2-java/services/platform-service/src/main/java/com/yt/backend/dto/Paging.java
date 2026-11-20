package com.yt.backend.dto;

import com.yt.backend.common.Json;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** page / limit tu query string (sai thi dung mac dinh), gioi han 1..100 ban ghi moi trang */
public record Paging(int page, int size) {

    public static Paging of(String page, String limit) {
        int size = Math.min(Math.max(Json.parseIntLoose(limit, 20), 1), 100);
        int current = Math.max(Json.parseIntLoose(page, 1), 1);
        return new Paging(current, size);
    }

    public int offset() {
        return (page - 1) * size;
    }

    /** Pageable cua Spring Data (trang tinh tu 0) */
    public Pageable pageable(Sort sort) {
        return PageRequest.of(page - 1, size, sort);
    }
}
