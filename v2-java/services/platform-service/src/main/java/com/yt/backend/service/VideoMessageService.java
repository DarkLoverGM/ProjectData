package com.yt.backend.service;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.common.MongoUtils;
import com.yt.backend.entity.VideoMessage;
import com.yt.backend.repository.VideoMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Trao doi giua nguoi dang va admin ve 1 video (collection video_messages, services/VideoMessageServices.js).
 * - Nguoi dang gui tu Studio -> video co `appeal.pending` de admin thay can tra loi
 * - Admin tra loi trong man duyet video -> nguoi dang nhan thong bao kem link ve Studio
 * role: 'creator' | 'admin' | 'system' (tin tu dong, vd. khi nguoi dang thay file video)
 */
@Service
public class VideoMessageService {

    private static final int MAX_MESSAGE = 1000;
    // Nguoi dang gui lien tiep qua so nay ma admin chua tra loi thi phai cho (chong spam)
    private static final int MAX_UNANSWERED = 5;

    private final VideoMessageRepository messages;

    public VideoMessageService(VideoMessageRepository messages) {
        this.messages = messages;
    }

    public List<VideoMessage> list(String videoId) {
        return messages.findByVideoIdOrderByCreatedAtAsc(videoId);
    }

    /** videoId -> so tin */
    public Map<String, Long> countByVideo(Collection<String> videoIds) {
        if (videoIds.isEmpty()) return Map.of();
        return messages.findByVideoIdIn(videoIds).stream()
                .collect(Collectors.groupingBy(VideoMessage::getVideoId, Collectors.counting()));
    }

    /**
     * Them 1 tin. sender co the null (tin he thong khong ro nguoi gui).
     * @throws ApiException 400 noi dung trong / qua dai / nguoi dang gui lien tiep qua nhieu
     */
    public VideoMessage add(String videoId, String role, AuthUser sender, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw ApiException.badRequest("Nội dung trống");
        if (text.length() > MAX_MESSAGE) throw ApiException.badRequest("Tối đa " + MAX_MESSAGE + " ký tự");

        if ("creator".equals(role)) {
            List<VideoMessage> recent = messages.findByVideoIdAndRoleNotOrderByCreatedAtDesc(videoId, "system",
                    PageRequest.of(0, MAX_UNANSWERED));
            if (recent.size() >= MAX_UNANSWERED && recent.stream().allMatch(m -> "creator".equals(m.getRole()))) {
                throw ApiException.badRequest("Bạn đã gửi nhiều tin liên tiếp, vui lòng chờ admin phản hồi");
            }
        }

        VideoMessage m = new VideoMessage();
        m.setId(MongoUtils.uuid());
        m.setVideoId(videoId);
        m.setRole(role);
        m.setSenderId(sender == null ? null : sender.id());
        m.setSenderName(sender == null ? null : sender.username());
        m.setContent(text);
        m.setCreatedAt(new Date());
        return messages.insert(m);
    }
}
