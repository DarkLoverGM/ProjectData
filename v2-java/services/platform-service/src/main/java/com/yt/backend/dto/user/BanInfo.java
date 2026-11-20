package com.yt.backend.dto.user;

import com.yt.backend.auth.BanList;

import java.util.Date;

/** Thong tin khoa tai khoan trong JSON: { until, permanent, reason }, null = khong bi khoa */
public record BanInfo(Date until, boolean permanent, String reason) {

    public static BanInfo from(BanList.Ban ban) {
        return ban == null ? null : new BanInfo(ban.until(), ban.permanent(), ban.reason());
    }
}
