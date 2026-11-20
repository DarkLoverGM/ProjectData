package com.yt.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.common.Json;
import com.yt.backend.common.MongoUtils;
import com.yt.backend.dto.video.TagCount;
import com.yt.backend.dto.video.TagDto;
import com.yt.backend.entity.Tag;
import com.yt.backend.entity.Video;
import com.yt.backend.entity.VideoTag;
import com.yt.backend.repository.TagRepository;
import com.yt.backend.repository.VideoTagRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.StringOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/** Tag cua video (tags + video_tags) */
@Service
public class TagService {

    public static final int MAX_TAGS_PER_VIDEO = 10;
    public static final int MAX_TAG_LENGTH = 30;
    /** Sap theo ten khong phan biet hoa thuong (giong ORDER BY name cua MySQL cu) */
    private static final Comparator<TagDto> BY_NAME = Comparator.comparing(t -> t.name().toLowerCase(Locale.ROOT));

    private final TagRepository tags;
    private final VideoTagRepository videoTags;
    private final MongoTemplate mongo;
    private final ObjectMapper mapper = new ObjectMapper();

    public TagService(TagRepository tags, VideoTagRepository videoTags, MongoTemplate mongo) {
        this.tags = tags;
        this.videoTags = videoTags;
        this.mongo = mongo;
    }

    /** "Hanh Dong" -> "hanh-dong". Slug dung de so trung tag va lam URL. */
    public static String slugifyTag(String name) {
        String s = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036f]", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return s.length() > 40 ? s.substring(0, 40) : s;
    }

    /**
     * Nhan tag tu client (chuoi JSON hoac chuoi cach nhau boi dau phay),
     * tra ve [{ name, slug }] da bo trung, bo rong, toi da MAX_TAGS_PER_VIDEO.
     */
    public List<TagDto> normalizeTags(String input) {
        if (input == null) return List.of();
        List<String> list = new ArrayList<>();
        try {
            JsonNode node = mapper.readTree(input);
            if (node == null || !node.isArray()) return List.of();
            for (JsonNode n : node) list.add(n.isNull() ? "" : n.asText());
        } catch (Exception e) {
            list.addAll(Arrays.asList(input.split(",", -1)));
        }

        Set<String> seen = new HashSet<>();
        List<TagDto> result = new ArrayList<>();
        for (String raw : list) {
            String name = raw.replaceFirst("^#+", "").replaceAll("\\s+", " ").trim();
            if (name.length() > MAX_TAG_LENGTH) name = name.substring(0, MAX_TAG_LENGTH);
            String slug = slugifyTag(name);
            if (slug.isEmpty() || !seen.add(slug)) continue;
            result.add(new TagDto(name, slug));
            if (result.size() >= MAX_TAGS_PER_VIDEO) break;
        }
        return result;
    }

    /** Ghi de toan bo tag cua video; tag chua co thi tao moi. */
    public List<TagDto> setVideoTags(String videoId, String input) {
        List<TagDto> wanted = normalizeTags(input);
        videoTags.deleteByVideoId(videoId);
        if (wanted.isEmpty()) return List.of();

        // Slug trung thi giu tag cu (unique index slug), thay cho INSERT IGNORE
        Date now = new Date();
        BulkOperations bulk = mongo.bulkOps(BulkOperations.BulkMode.UNORDERED, Tag.class);
        for (TagDto t : wanted) {
            bulk.upsert(Query.query(Criteria.where("slug").is(t.slug())), new Update()
                    .setOnInsert("id", MongoUtils.uuid())
                    .setOnInsert("name", t.name())
                    .setOnInsert("created_at", now));
        }
        MongoUtils.ignoreDuplicate(bulk::execute);

        List<VideoTag> links = tags.findBySlugIn(wanted.stream().map(TagDto::slug).toList()).stream().map(tag -> {
            VideoTag link = new VideoTag();
            link.setVideoId(videoId);
            link.setTagId(tag.getId());
            return link;
        }).toList();
        MongoUtils.ignoreDuplicate(() -> videoTags.insert(links));
        return getTagsByVideoIds(List.of(videoId)).getOrDefault(videoId, List.of());
    }

    /** { videoId: [{ name, slug }] }, tag trong moi video sap theo ten */
    public Map<String, List<TagDto>> getTagsByVideoIds(Collection<String> videoIds) {
        Map<String, List<TagDto>> map = new HashMap<>();
        if (videoIds.isEmpty()) return map;
        List<VideoTag> links = videoTags.findByVideoIdIn(videoIds);
        Map<String, Tag> byId = tags.findByIdIn(links.stream().map(VideoTag::getTagId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> a));
        for (VideoTag link : links) {
            Tag tag = byId.get(link.getTagId());
            if (tag == null) continue; // link toi tag khong con ton tai (nhu INNER JOIN)
            map.computeIfAbsent(link.getVideoId(), k -> new ArrayList<>()).add(new TagDto(tag.getName(), tag.getSlug()));
        }
        map.values().forEach(list -> list.sort(BY_NAME));
        return map;
    }

    public Tag getTagBySlug(String slug) {
        String s = slugifyTag(slug);
        return s.isEmpty() ? null : tags.findOneBySlug(s).orElse(null);
    }

    /** Tag duoc dung nhieu nhat (chi tinh video dang hien thi va cong khai) */
    public List<TagCount> getPopularTags(int limit) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.lookup("videos", "video_id", "id", "video"),
                Aggregation.match(Criteria.where("video.status").is(1).and("video.visibility").nin(Video.UNLISTED, Video.PRIVATE)),
                Aggregation.group("tag_id").count().as("count"),
                Aggregation.lookup("tags", "_id", "id", "tag"),
                Aggregation.unwind("tag"),
                Aggregation.project("count").and("tag.name").as("name").and("tag.slug").as("slug")
                        .and(StringOperators.valueOf("tag.name").toLower()).as("nameKey"),
                Aggregation.sort(Sort.by(Sort.Order.desc("count"), Sort.Order.asc("nameKey"))),
                Aggregation.limit(Math.max(limit, 1)));
        return mongo.aggregate(agg, "video_tags", TagCount.class).getMappedResults();
    }

    /** Goi y khi dang go tag: khop dau slug, tag dung nhieu xep truoc */
    public List<TagCount> suggestTags(String text, int limit) {
        String slug = slugifyTag(text);
        if (slug.isEmpty()) return List.of();
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("slug").regex("^" + MongoUtils.escapeRegex(slug))), // thay cho LIKE 'slug%'
                Aggregation.lookup("video_tags", "id", "tag_id", "links"),
                Aggregation.project("name", "slug")
                        .and(ArrayOperators.Size.lengthOfArray("links")).as("count")
                        .and(StringOperators.valueOf("name").toLower()).as("nameKey"),
                Aggregation.sort(Sort.by(Sort.Order.desc("count"), Sort.Order.asc("nameKey"))),
                Aggregation.limit(Math.max(limit, 1)));
        return mongo.aggregate(agg, "tags", TagCount.class).getMappedResults();
    }

    /** toLimit cua Route/tag.js: parseInt || fallback, kep trong [1, max] */
    public static int toLimit(String value, int fallback, int max) {
        return Math.min(max, Math.max(1, Json.parseIntLoose(value, fallback)));
    }
}
