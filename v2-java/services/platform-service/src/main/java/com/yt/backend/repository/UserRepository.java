package com.yt.backend.repository;

import com.yt.backend.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Tai khoan. Email / username so khong phan biet hoa thuong (collation strength 2, giong MySQL cu). */
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findOneById(String id);

    List<User> findByIdIn(Collection<String> ids);

    boolean existsOneById(String id);

    Optional<User> findOneByGoogleId(String googleId);

    @Query(value = "{ 'email': ?0 }", collation = "{ 'locale': 'en', 'strength': 2 }")
    Optional<User> findOneByEmailIgnoreCase(String email);

    @Query(value = "{ 'username': ?0 }", collation = "{ 'locale': 'en', 'strength': 2 }", exists = true)
    boolean existsByUsernameIgnoreCase(String username);

    @Query(value = "{ 'email': ?0, 'id': { '$ne': ?1 } }", collation = "{ 'locale': 'en', 'strength': 2 }", exists = true)
    boolean existsByEmailIgnoreCaseAndIdNot(String email, String id);

    @Query(value = "{ 'username': ?0, 'id': { '$ne': ?1 } }", collation = "{ 'locale': 'en', 'strength': 2 }", exists = true)
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, String id);

    List<User> findByBannedUntilAfter(Date now);

    long countByRole(String role);

    long countByBannedUntilAfter(Date now);

    long deleteOneById(String id);
}
