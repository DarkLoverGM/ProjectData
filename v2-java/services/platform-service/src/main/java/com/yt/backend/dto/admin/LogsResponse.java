package com.yt.backend.dto.admin;

import java.util.List;
import java.util.Map;

/** Log truy cap (file server/data/log.json, moi dong la object tu do) */
public record LogsResponse(List<Map<String, Object>> items, long total) {}
