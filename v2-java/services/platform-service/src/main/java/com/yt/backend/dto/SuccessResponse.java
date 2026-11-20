package com.yt.backend.dto;

/** { success } - cho cac thao tac khong can tra them gi */
public record SuccessResponse(boolean success) {

    public static final SuccessResponse OK = new SuccessResponse(true);
}
