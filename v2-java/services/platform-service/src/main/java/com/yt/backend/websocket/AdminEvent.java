package com.yt.backend.websocket;

import com.yt.backend.dto.studio.VideoMessageDto;

/**
 * Su kien day xuong trang quan tri qua /ws/admin (server-java/src/main/resources/admin/admin.js).
 * Chi gui cho admin dang mo trang, de admin thay ngay viec can xu ly thay vi phai bam "Tai lai".
 */
public sealed interface AdminEvent {

    String type();

    /** Video vua dang (hau kiem: video da hien, admin duyet sau) */
    record VideoNew(String type, String videoId, String title, String uploader) implements AdminEvent {
        public VideoNew(String videoId, String title, String uploader) {
            this("video:new", videoId, title, uploader);
        }
    }

    /** Nguoi dang thay file video (thuong la sua video bi chan roi gui duyet lai) */
    record VideoMedia(String type, String videoId, String uploader) implements AdminEvent {
        public VideoMedia(String videoId, String uploader) {
            this("video:media", videoId, uploader);
        }
    }

    /** 1 tin trong luong trao doi admin <-> nguoi dang (ca tin admin khac vua tra loi) */
    record VideoMessage(String type, String videoId, VideoMessageDto message) implements AdminEvent {
        public VideoMessage(VideoMessageDto message) {
            this("video:message", message.videoId(), message);
        }
    }

    record CommentReported(String type, String reportId, String commentId, String videoId, String reason)
            implements AdminEvent {
        public CommentReported(String reportId, String commentId, String videoId, String reason) {
            this("report:comment", reportId, commentId, videoId, reason);
        }
    }

    record UserReported(String type, String reportId, String reportedUser, String reason) implements AdminEvent {
        public UserReported(String reportId, String reportedUser, String reason) {
            this("report:user", reportId, reportedUser, reason);
        }
    }

    record UserNew(String type, String userId, String username) implements AdminEvent {
        public UserNew(String userId, String username) {
            this("user:new", userId, username);
        }
    }

    /**
     * 1 admin vua thao tac (cung du lieu voi audit log): admin khac cap nhat danh sach / so dem,
     * va biet video / bao cao do da co nguoi xu ly.
     */
    record AdminAction(String type, String action, String targetType, String targetId, String targetLabel,
                       String adminId, String adminName) implements AdminEvent {
        public AdminAction(String action, String targetType, String targetId, String targetLabel,
                           String adminId, String adminName) {
            this("admin:action", action, targetType, targetId, targetLabel, adminId, adminName);
        }
    }
}
