package com.yt.user.controller;

import com.yt.user.dto.UserRequest;
import com.yt.user.dto.UserResponse;
import com.yt.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserResponse> getAll(@RequestParam(required = false) String role) {
        return service.getAll(role);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable String id, @Valid @RequestBody UserRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
