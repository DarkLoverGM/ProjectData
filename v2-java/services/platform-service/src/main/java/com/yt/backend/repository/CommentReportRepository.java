package com.yt.backend.repository;

import com.yt.backend.entity.CommentReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

/** Bao cao binh luan. */
public interface CommentReportRepository extends MongoRepository<CommentReport, String> {

    long countByStatus(int status);

    long countByCommentIdIn(Collection<String> commentIds);

    List<CommentReport> findByCommentIdIn(Collection<String> commentIds);

    long deleteByUserId(String userId);

    long deleteByVideoIdIn(Collection<String> videoIds);
}
