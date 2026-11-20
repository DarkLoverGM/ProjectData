package com.yt.backend.dto.video;

/** So like / dislike + cam xuc cua nguoi dang xem (userReaction: like | dislike | null) */
public record ReactionResponse(long like, long dislike, String userReaction, boolean liked, boolean disliked) {

    public static ReactionResponse of(VideoDto.Stats stats, String reaction) {
        return new ReactionResponse(stats.like(), stats.dislike(), reaction, "like".equals(reaction), "dislike".equals(reaction));
    }
}
