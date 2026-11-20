package com.yt.backend.dto.video;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.entity.User;
import com.yt.backend.entity.Video;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Video dang long nhau ma frontend dang dung (VideoCard, Watch, Shorts...):
 * { id, info: {...}, media: { src, thumb }, uploader: { user_id, name, avatar }, stats: {...}, tags, video_group, status, membersOnly, visibility }
 */
public record VideoDto(String id, Info info, Media media, Uploader uploader, Stats stats, List<TagDto> tags,
                       @JsonProperty("video_group") String videoGroup, Integer status, boolean membersOnly,
                       String visibility) {

    /** speciallist: moc OP/ED { op?: {start, end}, ep?: {start, end} }, video khong co thi null */
    public record Info(String title, String slug, String subtitle, Map<String, Object> speciallist,
                       String description, Date uploadedAt) {}

    public record Media(String src, String thumb) {}

    public record Uploader(@JsonProperty("user_id") String userId, String name, String avatar) {}

    public record Stats(long like, long dislike, long view) {}

    private static String text(String s) {
        return s == null ? "" : s;
    }

    /**
     * @param uploader nguoi dang (null -> ten / avatar rong)
     * @param like     so like (danh sach khong dem -> 0)
     */
    public static VideoDto of(Video v, User uploader, long like, long dislike, List<TagDto> tags) {
        // Video chi danh cho hoi vien: khong lo link file, VideoController.watch tu gan lai khi nguoi xem co quyen
        String src = v.isMembersOnly() ? "" : text(v.getSrc());
        return new VideoDto(
                v.getId(),
                new Info(text(v.getTitle()), text(v.getSlug()), text(v.getSubtitle()), v.getSpeciallist(),
                        text(v.getDescription()), v.getUploadAt()),
                new Media(src, text(v.getThumbnail())),
                new Uploader(v.getUserId(), uploader == null ? "" : text(uploader.getUsername()),
                        uploader == null ? "" : text(uploader.getAvatar())),
                new Stats(like, dislike, v.getView() == null ? 0 : v.getView()),
                tags == null ? List.of() : tags,
                v.getVideoGroup() == null || v.getVideoGroup().isEmpty() ? Video.GROUP_MAIN : v.getVideoGroup(),
                v.getStatus(),
                v.isMembersOnly(),
                v.visibilityOrDefault());
    }

    /** Ban sao co link file (hoi vien / chu kenh xem video chi danh cho hoi vien) */
    public VideoDto withSrc(String src) {
        return new VideoDto(id, info, new Media(src, media.thumb()), uploader, stats, tags, videoGroup, status, membersOnly, visibility);
    }

    public boolean isPrivate() {
        return Video.PRIVATE.equals(visibility);
    }

    public String uploaderId() {
        return uploader == null ? null : uploader.userId();
    }
}
