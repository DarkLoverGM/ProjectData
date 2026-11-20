package com.yt.backend.dto.membership;

/** hidden = true: goi dang co hoi vien nen chi bi an, khong xoa */
public record DeleteTierResponse(boolean success, boolean hidden, String message) {}
