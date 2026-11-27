package com.yt.user.service;

import com.yt.user.dto.UserRequest;
import com.yt.user.dto.UserResponse;
import com.yt.user.entity.User;
import com.yt.user.exception.BadRequestException;
import com.yt.user.exception.ResourceNotFoundException;
import com.yt.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getChannelName(),
                user.getAvatar(),
                user.getBio(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    public List<UserResponse> getAll(String role) {
        List<User> users = (role != null && !role.isBlank())
                ? repository.findByRole(role.toUpperCase())
                : repository.findAll();
        return users.stream().map(this::toResponse).toList();
    }

    public UserResponse getById(String id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + id));
        return toResponse(user);
    }

    public UserResponse create(UserRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new BadRequestException("Email đã được sử dụng: " + request.email());
        }
        if (repository.existsByUsername(request.username())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại: " + request.username());
        }

        User user = new User(
                "user-" + UUID.randomUUID().toString().substring(0, 8),
                request.username().trim(),
                request.email().trim().toLowerCase(),
                request.channelName().trim(),
                (request.avatar() == null || request.avatar().isBlank())
                        ? "https://api.dicebear.com/7.x/initials/svg?seed=" + request.username().trim()
                        : request.avatar().trim(),
                request.bio() == null ? "" : request.bio().trim(),
                request.role().toUpperCase(),
                Instant.now()
        );
        return toResponse(repository.save(user));
    }

    public UserResponse update(String id, UserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + id));

        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setChannelName(request.channelName().trim());
        if (request.avatar() != null && !request.avatar().isBlank()) {
            user.setAvatar(request.avatar().trim());
        }
        user.setBio(request.bio() == null ? "" : request.bio().trim());
        user.setRole(request.role().toUpperCase());

        return toResponse(repository.save(user));
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng: " + id);
        }
        repository.deleteById(id);
    }
}
