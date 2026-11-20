package com.yt.backend.dto.video;

/** Tag kem so video dang dung no: { name, slug, count } (tag pho bien / goi y khi go tag) */
public record TagCount(String name, String slug, long count) {}
