package com.yt.user.repository;

import com.yt.user.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByRole(String role);
}
