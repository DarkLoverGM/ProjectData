package com.yt.backend.dto.comment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.List;

/**
 * 1 binh luan cho frontend (components/comment/Comment.js).
 * - superThanks: so tien (VND) neu la binh luan Super Thanks, khong thi null
 * - memberTier: ten goi hoi vien cua nguoi binh luan o kenh cua video (huy hieu), khong thi null
 * - userReaction: cam xuc cua nguoi dang xem (like | dislike | null)
 * - replies: chi binh luan goc moi co (cay 2 tang)
 */
public record CommentDto(String id, String videoId, String parentId, String userId, String username, String avatar,
                         String text, Date createdAt, long like, long dislike, Long superThanks, String memberTier,
                         String userReaction,
                         @JsonInclude(JsonInclude.Include.NON_NULL) List<CommentDto> replies) {

    public CommentDto withUserReaction(String reaction) {
        return new CommentDto(id, videoId, parentId, userId, username, avatar, text, createdAt, like, dislike,
                superThanks, memberTier, reaction, replies);
    }

    public CommentDto withReplies(List<CommentDto> list) {
        return new CommentDto(id, videoId, parentId, userId, username, avatar, text, createdAt, like, dislike,
                superThanks, memberTier, userReaction, list);
    }

    @JsonIgnore
    public boolean isReply() {
        return parentId != null && !parentId.isEmpty();
    }
}
