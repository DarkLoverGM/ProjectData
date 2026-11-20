package com.yt.backend.repository;

import com.yt.backend.entity.AdminAudit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Nhat ky admin. */
public interface AdminAuditRepository extends MongoRepository<AdminAudit, String> {

    List<AdminAudit> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId, Pageable pageable);

    List<AdminAudit> findByTargetIdOrderByCreatedAtDesc(String targetId, Pageable pageable);
}
