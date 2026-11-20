package com.yt.backend.service;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.MongoUtils;
import com.yt.backend.dto.video.GroupedVideos;
import com.yt.backend.dto.video.TagDto;
import com.yt.backend.dto.video.VideoDto;
import com.yt.backend.entity.Library;
import com.yt.backend.entity.LibraryItem;
import com.yt.backend.entity.Tag;
import com.yt.backend.entity.User;
import com.yt.backend.entity.Video;
import com.yt.backend.entity.VideoReaction;
import com.yt.backend.entity.VideoTag;
import com.yt.backend.repository.LibraryItemRepository;
import com.yt.backend.repository.LibraryRepository;
import com.yt.backend.repository.TagRepository;
import com.yt.backend.repository.UserRepository;
import com.yt.backend.repository.VideoReactionRepository;
import com.yt.backend.repository.VideoRepository;
import com.yt.backend.repository.VideoTagRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Xem video, like / dislike, luot xem, dang video, thu vien, tim kiem */
@Service
public class VideoService {

    private static final String LIKE = "like";
    private static final String DISLIKE = "dislike";

    private final VideoRepository videos;
    private final UserRepository users;
    private final VideoReactionRepository reactions;
    private final VideoTagRepository videoTags;
    private final TagRepository tags;
    private final LibraryRepository libraries;
    private final LibraryItemRepository libraryItems;
    private final TagService tagService;
    private final MongoTemplate mongo;

    public VideoService(VideoRepository videos, UserRepository users, VideoReactionRepository reactions,
                        VideoTagRepository videoTags, TagRepository tags, LibraryRepository libraries,
                        LibraryItemRepository libraryItems, TagService tagService, MongoTemplate mongo) {
        this.videos = videos;
        this.users = users;
        this.reactions = reactions;
        this.videoTags = videoTags;
        this.tags = tags;
        this.libraries = libraries;
        this.libraryItems = libraryItems;
        this.tagService = tagService;
        this.mongo = mongo;
    }

    // ------------------------------------------------------------------
    // ENTITY -> DTO
    // ------------------------------------------------------------------

    /**
     * Doi danh sach video sang DTO kem nguoi dang + tag (1 query moi loai, thay cho JOIN).
     * Giong INNER JOIN cu: video khong tim thay nguoi dang thi bi bo ra.
     */
    private List<VideoDto> toDtos(List<Video> list) {
        Map<String, User> uploaders = users.findByIdIn(list.stream().map(Video::getUserId).filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<String, List<TagDto>> tagMap = tagService.getTagsByVideoIds(list.stream().map(Video::getId).toList());
        List<VideoDto> out = new ArrayList<>();
        for (Video v : list) {
            User u = uploaders.get(v.getUserId());
            if (u == null) continue;
            out.add(VideoDto.of(v, u, 0, 0, tagMap.get(v.getId())));
        }
        return out;
    }

    // ------------------------------------------------------------------
    // READ
    // ------------------------------------------------------------------

    public GroupedVideos getAllVideos() {
        return GroupedVideos.split(toDtos(videos.findListed()), false);
    }

    /** { Main, Short } cac video co gan tag `tagId` */
    public GroupedVideos getVideosByTag(String tagId) {
        List<String> ids = videoTags.findByTagId(tagId).stream().map(VideoTag::getVideoId).toList();
        if (ids.isEmpty()) return GroupedVideos.EMPTY;
        return GroupedVideos.split(toDtos(videos.findListedByIdIn(ids)), true);
    }

    public List<VideoDto> getAllVideosByUser(String userId) {
        return toDtos(videos.findListedByUserId(userId));
    }

    /** Tim 1 video theo id (uu tien) hoac slug, kem so like / dislike. Khong co thi null */
    public VideoDto getVideo(String slug, String id) {
        Optional<Video> found;
        if (id != null && !id.isEmpty()) found = videos.findOneById(id);
        else if (slug != null && !slug.isEmpty()) found = videos.findFirstBySlug(slug);
        else return null;

        Video v = found.orElse(null);
        if (v == null) return null;
        User uploader = v.getUserId() == null ? null : users.findOneById(v.getUserId()).orElse(null);
        if (uploader == null) return null;
        List<TagDto> videoTagList = tagService.getTagsByVideoIds(List.of(v.getId())).get(v.getId());
        return VideoDto.of(v, uploader, reactions.countByVideoIdAndAction(v.getId(), LIKE),
                reactions.countByVideoIdAndAction(v.getId(), DISLIKE), videoTagList);
    }

    /** Link file goc cua video (VideoDto an di voi video chi danh cho hoi vien) */
    public String getVideoSrc(String videoId) {
        return videos.findOneById(videoId).map(Video::getSrc).orElse("");
    }

    // ------------------------------------------------------------------
    // NEXT / PREV
    // ------------------------------------------------------------------

    /**
     * Chon video lien quan nhat theo huong `direction` (1 = next, -1 = prev).
     * Diem: moi tag trung +10, cung nguoi dang +1. Bang diem thi lay video gan nhat
     * theo thu tu them vao (di theo huong), nen cac video cung chu de se xoay vong voi nhau.
     * `exclude`: id video vua xem, bo qua de khong bi lap A -> B -> A.
     */
    private VideoDto pickRelated(String currentId, int direction, List<String> exclude) {
        // Video dai dang hien, theo thu tu them vao (_id tang dan)
        List<Video> list = videos.findListedByVideoGroup(Video.GROUP_MAIN, Sort.by("mongoId"));
        if (list.isEmpty()) return null;

        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(currentId)) {
                idx = i;
                break;
            }
        }
        List<Video> others = list.stream().filter(v -> !v.getId().equals(currentId)).toList();
        if (others.isEmpty()) return null;
        Map<String, List<TagDto>> tagMap = tagService.getTagsByVideoIds(list.stream().map(Video::getId).toList());
        // Ket qua next / prev khong kem ten / avatar nguoi dang (giong ban cu)
        Function<Video, VideoDto> dto = v -> VideoDto.of(v, null, 0, 0, tagMap.get(v.getId()));
        if (idx == -1) return dto.apply(others.get(ThreadLocalRandom.current().nextInt(others.size())));

        Video current = list.get(idx);
        Set<String> tagSet = tagMap.getOrDefault(current.getId(), List.of()).stream().map(TagDto::slug).collect(Collectors.toSet());
        Map<String, Integer> pos = new HashMap<>();
        for (int i = 0; i < list.size(); i++) pos.put(list.get(i).getId(), i);
        int n = list.size();
        int from = idx;

        Comparator<Video> byScore = Comparator.comparingInt(v -> {
            long same = tagMap.getOrDefault(v.getId(), List.of()).stream().filter(t -> tagSet.contains(t.slug())).count();
            return -(int) (same * 10 + (Objects.equals(v.getUserId(), current.getUserId()) ? 1 : 0));
        });
        Comparator<Video> byDistance = Comparator.comparingInt(v -> Math.floorMod((pos.get(v.getId()) - from) * direction, n));

        // Xem het ca roi thi bo qua exclude
        Set<String> skip = new HashSet<>(exclude);
        List<Video> fresh = others.stream().filter(v -> !skip.contains(v.getId())).toList();
        List<Video> candidates = new ArrayList<>(fresh.isEmpty() ? others : fresh);
        candidates.sort(byScore.thenComparing(byDistance));
        return dto.apply(candidates.get(0));
    }

    /** Video dai tiep theo, uu tien cung chu de. Short co trang /shorts rieng nen bo qua. */
    public VideoDto getNextVideo(String currentId, List<String> exclude) {
        return pickRelated(currentId, 1, exclude);
    }

    /** Video dai truoc do, uu tien cung chu de */
    public VideoDto getPrevVideo(String currentId, List<String> exclude) {
        return pickRelated(currentId, -1, exclude);
    }

    // ------------------------------------------------------------------
    // REACTIONS (like / dislike)
    // ------------------------------------------------------------------

    /** Moi user chi 1 cam xuc tren 1 video (unique index video_id + user_id): upsert thay vi insert */
    private VideoDto react(String videoId, String userId, String action) {
        MongoUtils.ignoreDuplicate(() -> mongo.upsert(
                Query.query(Criteria.where("video_id").is(videoId).and("user_id").is(userId)),
                new Update().set("action", action).setOnInsert("id", MongoUtils.uuid()),
                VideoReaction.class));
        return getVideo("", videoId);
    }

    private VideoDto removeReaction(String videoId, String userId, String action) {
        reactions.deleteByVideoIdAndUserIdAndAction(videoId, userId, action);
        return getVideo("", videoId);
    }

    /** action: like | unlike | dislike | undislike. Sai action thi null */
    public VideoDto applyReaction(String videoId, String userId, String action) {
        return switch (action) {
            case "like" -> react(videoId, userId, LIKE);
            case "dislike" -> react(videoId, userId, DISLIKE);
            case "unlike" -> removeReaction(videoId, userId, LIKE);
            case "undislike" -> removeReaction(videoId, userId, DISLIKE);
            default -> null;
        };
    }

    public String getUserReaction(String videoId, String userId) {
        if (videoId == null || userId == null) return null;
        return reactions.findOneByVideoIdAndUserId(videoId, userId).map(VideoReaction::getAction).orElse(null);
    }

    // ------------------------------------------------------------------
    // VIEW COUNT
    // ------------------------------------------------------------------

    public VideoDto incrementViewCount(String videoId) {
        if (videoId == null || videoId.isEmpty()) return null;
        // $inc se loi neu view dang la null (du lieu cu), nen dung pipeline: view = (view ?? 0) + 1
        AggregationUpdate inc = AggregationUpdate.update().set("view").toValue(
                ArithmeticOperators.Add.valueOf(ConditionalOperators.ifNull("view").then(0)).add(1));
        mongo.updateFirst(Query.query(Criteria.where("id").is(videoId)), inc, Video.class);
        return getVideo("", videoId);
    }

    // ------------------------------------------------------------------
    // UPLOAD
    // ------------------------------------------------------------------

    public static String slugOf(String title) {
        return (title == null ? "" : title).toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }

    public boolean slugTaken(String title) {
        return videos.findFirstBySlug(slugOf(title)).isPresent();
    }

    /** src / thumb la duong dan public, vd. /video/123.mp4 */
    public VideoDto uploadVideo(String title, String description, String src, String thumb,
                                AuthUser user, String group, String tagsJson) {
        Video v = new Video();
        v.setId(MongoUtils.uuid());
        v.setSrc(src == null ? "" : src);
        v.setTitle(title);
        v.setSlug(slugOf(title));
        v.setThumbnail(thumb == null ? "" : thumb);
        v.setUserId(user.id());
        v.setView(0L);
        v.setStatus(Video.VISIBLE);
        v.setUploadAt(new Date());
        v.setDescription(description == null || description.isEmpty() ? "No description" : description);
        v.setVideoGroup(Video.GROUP_SHORT.equals(group) ? Video.GROUP_SHORT : Video.GROUP_MAIN);
        videos.insert(v);
        tagService.setVideoTags(v.getId(), tagsJson);
        return getVideo("", v.getId());
    }

    // ------------------------------------------------------------------
    // LIBRARY (library + value_in_library)
    // ------------------------------------------------------------------

    /** Id thu vien mac dinh cua user, tao moi neu chua co (user cu). */
    public String getDefaultLibraryId(AuthUser user) {
        List<Library> existing = libraries.findByUserId(user.id(), Sort.by("id"));
        if (!existing.isEmpty()) return existing.get(0).getId();

        Library library = new Library();
        library.setId(MongoUtils.uuid());
        library.setUserId(user.id());
        library.setLibraryName(("Thư viện của " + (user.username() == null ? "" : user.username())).trim());
        libraries.insert(library);
        return library.getId();
    }

    private List<String> libraryIds(String userId) {
        return libraries.findByUserId(userId).stream().map(Library::getId).toList();
    }

    /** @return null neu thanh cong, nguoc lai ma loi (INVALID_INPUT, VIDEO_NOT_FOUND) */
    public String addVideoToLibrary(AuthUser user, String videoId) {
        if (videoId == null || videoId.isEmpty()) return "INVALID_INPUT";
        if (!videos.existsWatchableById(videoId)) return "VIDEO_NOT_FOUND";

        String libraryId = getDefaultLibraryId(user);
        // Da co thi giu nguyen (unique index library_id + video_id)
        MongoUtils.ignoreDuplicate(() -> mongo.upsert(
                Query.query(Criteria.where("library_id").is(libraryId).and("video_id").is(videoId)),
                new Update().setOnInsert("id", MongoUtils.uuid()).setOnInsert("createdAt", new Date()),
                LibraryItem.class));
        return null;
    }

    public void removeVideoFromLibrary(AuthUser user, String videoId) {
        if (videoId == null || videoId.isEmpty()) return;
        libraryItems.deleteByLibraryIdInAndVideoId(libraryIds(user.id()), videoId);
    }

    public boolean isVideoInLibrary(AuthUser user, String videoId) {
        if (videoId == null || videoId.isEmpty()) return false;
        return libraryItems.existsByLibraryIdInAndVideoId(libraryIds(user.id()), videoId);
    }

    /** { Main, Short } cac video user da luu, theo thu tu luu */
    public GroupedVideos getLibraryVideos(AuthUser user) {
        List<String> libIds = libraryIds(user.id());
        if (libIds.isEmpty()) return GroupedVideos.EMPTY;
        // Ban ghi cu chua co createdAt -> roi ve _id (ObjectId cung tang theo thoi gian insert)
        List<String> order = libraryItems.findByLibraryIdIn(libIds, Sort.by("createdAt", "mongoId")).stream()
                .map(LibraryItem::getVideoId).filter(Objects::nonNull).distinct().toList();
        if (order.isEmpty()) return GroupedVideos.EMPTY;
        List<Video> rows = new ArrayList<>(videos.findWatchableByIdIn(order));
        rows.sort(Comparator.comparingInt(v -> order.indexOf(v.getId())));
        return GroupedVideos.split(toDtos(rows), true);
    }

    // ------------------------------------------------------------------
    // SEARCH (GET /api/search)
    // ------------------------------------------------------------------

    /**
     * Tim video theo tieu de hoac tag (khong phan biet hoa thuong).
     * q = "random" -> lay ngau nhien 3 video dai + 6 short.
     */
    public GroupedVideos searchVideos(String q) {
        String keyword = q == null ? "" : q.trim();
        if (keyword.isEmpty()) return GroupedVideos.EMPTY;

        List<Video> rows;
        if ("random".equalsIgnoreCase(keyword)) {
            rows = new ArrayList<>(sample(Video.GROUP_MAIN, 3));
            rows.addAll(sample(Video.GROUP_SHORT, 6));
        } else {
            // Video co tag khop voi tu khoa (so theo slug, nen "Hanh dong" khop tag "hanh-dong")
            String slug = TagService.slugifyTag(keyword);
            List<String> tagIds = slug.isEmpty() ? List.of()
                    : tags.findBySlugRegex(MongoUtils.escapeRegex(slug)).stream().map(Tag::getId).toList();
            List<String> tagVideoIds = tagIds.isEmpty() ? List.of()
                    : videoTags.findByTagIdIn(tagIds).stream().map(VideoTag::getVideoId).toList();

            Query query = Query.query(Video.listed().orOperator(
                    MongoUtils.contains("title", keyword), Criteria.where("id").in(tagVideoIds))).limit(100);
            rows = mongo.find(query, Video.class);
        }
        return GroupedVideos.split(toDtos(rows), true);
    }

    private List<Video> sample(String group, int size) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Video.listed().and("video_group").is(group)),
                Aggregation.sample(size));
        return mongo.aggregate(agg, Video.class, Video.class).getMappedResults();
    }
}
