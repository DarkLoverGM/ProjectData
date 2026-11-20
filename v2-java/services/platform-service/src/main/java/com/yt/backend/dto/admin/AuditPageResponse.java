package com.yt.backend.dto.admin;

import java.util.List;

/** Trang nhat ky + danh sach loai hanh dong de loc */
public record AuditPageResponse(List<AuditDto> items, long total, int page, int limit, List<String> actions) {}
