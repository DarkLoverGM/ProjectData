package com.yt.backend.repository;

import com.yt.backend.entity.UserReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/** Bao cao nguoi dung. */
public interface UserReportRepository extends MongoRepository<UserReport, String> {

    @Query(value = "{ 'reporter': ?0, 'reported_user': ?1, 'comment_id': null, 'status': 'pending' }", exists = true)
    boolean existsPending(String reporter, String reportedUser);

    long countByStatus(String status);

    long countByReportedUserAndStatus(String reportedUser, String status);

    long countByReportedUser(String reportedUser);

    List<UserReport> findByReportedUserAndStatus(String reportedUser, String status);

    long deleteByReporterOrReportedUser(String reporter, String reportedUser);
}
