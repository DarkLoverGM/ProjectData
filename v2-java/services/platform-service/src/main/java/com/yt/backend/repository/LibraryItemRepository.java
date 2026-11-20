package com.yt.backend.repository;

import com.yt.backend.entity.LibraryItem;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Video trong thu vien. */
public interface LibraryItemRepository extends MongoRepository<LibraryItem, String> {

    Optional<LibraryItem> findOneByLibraryIdAndVideoId(String libraryId, String videoId);

    boolean existsByLibraryIdInAndVideoId(Collection<String> libraryIds, String videoId);

    List<LibraryItem> findByLibraryIdIn(Collection<String> libraryIds, Sort sort);

    long deleteByLibraryIdInAndVideoId(Collection<String> libraryIds, String videoId);

    long deleteByLibraryIdIn(Collection<String> libraryIds);

    long deleteByVideoIdIn(Collection<String> videoIds);
}
