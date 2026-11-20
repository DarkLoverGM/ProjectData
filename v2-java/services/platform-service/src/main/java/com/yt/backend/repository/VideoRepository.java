package com.yt.backend.repository;

import com.yt.backend.entity.Video;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Video. status: 1 = hien, 0 = an.
 * findListed...: video dang hien VA nguoi dang de cong khai (bo unlisted / private, xem Video.listed()).
 */
public interface VideoRepository extends MongoRepository<Video, String> {

    Optional<Video> findOneById(String id);

    Optional<Video> findOneByIdAndStatus(String id, int status);

    Optional<Video> findOneByIdAndUserId(String id, String userId);

    Optional<Video> findFirstBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, String id);

    List<Video> findByIdIn(Collection<String> ids);

    List<Video> findByUserId(String userId, Sort sort);

    String LISTED = "'status': 1, 'visibility': { $nin: ['unlisted', 'private'] }";

    @Query("{ " + LISTED + " }")
    List<Video> findListed();

    @Query("{ " + LISTED + ", 'id': { $in: ?0 } }")
    List<Video> findListedByIdIn(Collection<String> ids);

    @Query("{ " + LISTED + ", 'user_id': ?0 }")
    List<Video> findListedByUserId(String userId);

    @Query("{ " + LISTED + ", 'video_group': ?0 }")
    List<Video> findListedByVideoGroup(String videoGroup, Sort sort);

    /** Video xem duoc qua link (cong khai + khong cong khai), dung cho thu vien */
    @Query("{ 'status': 1, 'visibility': { $ne: 'private' }, 'id': { $in: ?0 } }")
    List<Video> findWatchableByIdIn(Collection<String> ids);

    @Query(value = "{ 'status': 1, 'visibility': { $ne: 'private' }, 'id': ?0 }", exists = true)
    boolean existsWatchableById(String id);

    long countByStatus(int status);

    long countByUserId(String userId);

    long countByUserIdAndStatus(String userId, int status);

    @Query(value = "{ 'user_id': ?0, 'moderation.state': ?1 }", count = true)
    long countByUserIdAndModerationState(String userId, String state);

    @Query(value = "{ 'appeal.pending': true }", count = true)
    long countAppealsPending();
}
