package com.yt.backend.websocket;

/** Su kien rieng cua 1 nguoi dung, gui qua /ws/user (frontend: src/utils/userRealtime.js) */
public sealed interface UserEvent {

    String type();

    /**
     * Video cua nguoi dang vua thay doi phia admin (admin tra loi, chan, hien lai).
     * Studio tai lai video do; title / content giong thong bao de hien ngay.
     */
    record StudioVideo(String type, String videoId, String title, String content) implements UserEvent {
        public StudioVideo(String videoId, String title, String content) {
            this("studio:video", videoId, title, content);
        }
    }
}
