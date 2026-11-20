package com.yt.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.dto.subtitle.SubtitleDto;
import com.yt.backend.entity.Subtitle;
import com.yt.backend.entity.Video;
import com.yt.backend.repository.SubtitleRepository;
import com.yt.backend.repository.VideoRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;

/** Phu de (CC) cua video, collection subtitles (services/SubtitleServices.js) */
@Service
public class SubtitleService {

    // Ngon ngu phu de duoc ho tro (thu tu nay la thu tu hien trong player / trang soan)
    public static final Map<String, String> SUBTITLE_LANGS = new LinkedHashMap<>();

    static {
        SUBTITLE_LANGS.put("vi", "Tiếng Việt");
        SUBTITLE_LANGS.put("en", "English");
    }

    private static final int MAX_CUES = 5000; // ~ video 3-4 tieng
    private static final int MAX_TEXT_LENGTH = 500;

    private final SubtitleRepository subtitles;
    private final VideoRepository videos;
    private final MongoTemplate mongo;

    public SubtitleService(SubtitleRepository subtitles, VideoRepository videos, MongoTemplate mongo) {
        this.subtitles = subtitles;
        this.videos = videos;
        this.mongo = mongo;
    }

    /** So huu han nhu Number() cua JS: so, hoac chuoi so; con lai NaN */
    private static double toNumber(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return Double.NaN;
        if (n.isNumber()) return n.asDouble();
        if (n.isTextual()) {
            String s = n.asText().trim();
            if (s.isEmpty()) return 0;
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    private static double round3(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    /**
     * Kiem tra + chuan hoa danh sach dong phu de do client gui len (thoi gian tinh bang giay).
     * @return cues da sap theo thoi gian bat dau
     * @throws ApiException 400 neu du lieu sai
     */
    private List<Subtitle.Cue> normalizeCues(JsonNode cues) {
        if (cues == null || !cues.isArray()) throw ApiException.badRequest("Dữ liệu phụ đề không hợp lệ");
        if (cues.size() > MAX_CUES) throw ApiException.badRequest("Tối đa " + MAX_CUES + " dòng phụ đề");

        List<Subtitle.Cue> result = new ArrayList<>();
        for (int i = 0; i < cues.size(); i++) {
            JsonNode cue = cues.get(i);
            double start = toNumber(cue.get("start"));
            double end = toNumber(cue.get("end"));
            JsonNode textNode = cue.get("text");
            String text = textNode == null || textNode.isNull() ? "" : textNode.asText().trim();
            if (text.isEmpty()) continue; // dong trong: bo qua thay vi bao loi
            if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end <= start) {
                throw ApiException.badRequest("Dòng " + (i + 1) + ": thời gian kết thúc phải lớn hơn thời gian bắt đầu");
            }
            if (text.length() > MAX_TEXT_LENGTH) {
                throw ApiException.badRequest("Dòng " + (i + 1) + ": tối đa " + MAX_TEXT_LENGTH + " ký tự");
            }
            // Lam tron toi mili giay cho gon DB
            result.add(new Subtitle.Cue(round3(start), round3(end), text));
        }
        result.sort(Comparator.comparingDouble(Subtitle.Cue::getStart).thenComparingDouble(Subtitle.Cue::getEnd));
        return result;
    }

    /** Phu de cua video theo thu tu SUBTITLE_LANGS */
    public List<SubtitleDto> getSubtitles(String videoId) {
        Map<String, Subtitle> byLang = new HashMap<>();
        for (Subtitle s : subtitles.findByVideoId(videoId)) byLang.put(s.getLang(), s);

        List<SubtitleDto> result = new ArrayList<>();
        SUBTITLE_LANGS.forEach((lang, label) -> {
            Subtitle s = byLang.get(lang);
            if (s != null) result.add(new SubtitleDto(lang, label, s.getCues(), s.getUpdatedAt()));
        });
        return result;
    }

    /**
     * Ghi de phu de 1 ngon ngu. Danh sach rong = xoa phu de ngon ngu do.
     * @return so dong da luu
     */
    public int saveSubtitles(String videoId, String lang, JsonNode cues, String userId) {
        if (!SUBTITLE_LANGS.containsKey(lang)) throw ApiException.badRequest("Ngôn ngữ không được hỗ trợ");

        List<Subtitle.Cue> normalized = normalizeCues(cues);
        if (normalized.isEmpty()) {
            deleteSubtitles(videoId, lang);
            return 0;
        }
        // Thay cho ON DUPLICATE KEY UPDATE (unique index video_id + lang); cues luu thang dang mang
        mongo.upsert(Query.query(Criteria.where("video_id").is(videoId).and("lang").is(lang)),
                new Update().set("cues", normalized).set("updated_at", new Date()).set("updated_by", userId),
                Subtitle.class);
        return normalized.size();
    }

    public void deleteSubtitles(String videoId, String lang) {
        subtitles.deleteByVideoIdAndLang(videoId, lang);
    }

    /** Chi nguoi dang video hoac admin duoc sua phu de. @throws ApiException 404 / 403 */
    public void checkEditPermission(String videoId, AuthUser user) {
        Video video = videos.findOneById(videoId).orElseThrow(() -> ApiException.notFound("Video không tồn tại"));
        // Thieu id o 1 trong 2 phia thi khong cho (Objects.equals(null, null) = true)
        boolean owner = user.id() != null && user.id().equals(video.getUserId());
        if (!"admin".equals(user.role()) && !owner) {
            throw ApiException.forbidden("Chỉ người đăng video mới được sửa phụ đề");
        }
    }
}
