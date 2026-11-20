package com.yt.backend.dto.admin;

import java.util.Date;

public record BanResponse(boolean success, String username, Date until, boolean permanent, long resolvedReports) {}
