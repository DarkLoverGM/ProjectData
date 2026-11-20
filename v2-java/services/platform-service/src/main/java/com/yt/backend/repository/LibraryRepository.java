package com.yt.backend.repository;

import com.yt.backend.entity.Library;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Thu vien. */
public interface LibraryRepository extends MongoRepository<Library, String> {

    List<Library> findByUserId(String userId);

    List<Library> findByUserId(String userId, Sort sort);

    long deleteByUserId(String userId);
}
