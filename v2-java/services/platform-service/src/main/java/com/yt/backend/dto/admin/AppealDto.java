package com.yt.backend.dto.admin;

import com.yt.backend.entity.Video;

import java.util.Date;

public record AppealDto(Boolean pending, Date at) {

    public static AppealDto from(Video.Appeal a) {
        return a == null ? null : new AppealDto(a.getPending(), a.getAt());
    }
}
