package com.yt.backend.repository;

import com.yt.backend.entity.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Tag. */
public interface TagRepository extends MongoRepository<Tag, String> {

    Optional<Tag> findOneBySlug(String slug);

    List<Tag> findBySlugIn(Collection<String> slugs);

    List<Tag> findByIdIn(Collection<String> ids);

    @Query("{ 'slug': { '$regex': ?0 } }")
    List<Tag> findBySlugRegex(String regex);
}
