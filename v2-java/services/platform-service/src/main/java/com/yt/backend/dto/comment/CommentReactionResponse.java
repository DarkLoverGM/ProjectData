package com.yt.backend.dto.comment;

public record CommentReactionResponse(String commentId, long like, long dislike, String userReaction) {}
