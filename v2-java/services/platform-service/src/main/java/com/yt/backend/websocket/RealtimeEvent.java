package com.yt.backend.websocket;

import com.yt.backend.dto.comment.CommentDto;

/**
 * Cac su kien server day xuong trinh duyet qua WebSocket (frontend: src/utils/realtime.js).
 * Moi su kien co `type` de client phan biet.
 */
public sealed interface RealtimeEvent {

    String type();

    /** Luot xem / like / dislike moi cua 1 video */
    record VideoStats(String type, String videoId, long view, long like, long dislike) implements RealtimeEvent {
        public VideoStats(String videoId, long view, long like, long dislike) {
            this("video:stats", videoId, view, like, dislike);
        }
    }

    /** Binh luan moi (ca binh luan Super Thanks) */
    record CommentNew(String type, String videoId, CommentDto comment) implements RealtimeEvent {
        public CommentNew(String videoId, CommentDto comment) {
            this("comment:new", videoId, comment);
        }
    }

    /** So like / dislike moi cua 1 binh luan (cam xuc rieng tung nguoi khong gui) */
    record CommentReaction(String type, String videoId, String commentId, long like, long dislike) implements RealtimeEvent {
        public CommentReaction(String videoId, String commentId, long like, long dislike) {
            this("comment:reaction", videoId, commentId, like, dislike);
        }
    }

    /** Binh luan bi admin go */
    record CommentDeleted(String type, String videoId, String commentId) implements RealtimeEvent {
        public CommentDeleted(String videoId, String commentId) {
            this("comment:deleted", videoId, commentId);
        }
    }
}
