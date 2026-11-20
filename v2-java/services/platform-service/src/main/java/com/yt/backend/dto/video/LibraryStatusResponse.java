package com.yt.backend.dto.video;

import com.fasterxml.jackson.annotation.JsonInclude;

/** { message?, saved } */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LibraryStatusResponse(String message, boolean saved) {}
