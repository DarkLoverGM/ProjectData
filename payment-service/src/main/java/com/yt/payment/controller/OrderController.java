package com.yt.payment.controller;

import com.yt.payment.dto.*;
import com.yt.payment.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    // API kiểm tra giao tiếp đồng bộ giữa Payment Service -> Video Service (Câu B)
    @GetMapping("/video/{videoId}")
    public VideoResponse getVideo(@PathVariable String videoId) {
        return service.getVideo(videoId);
    }

    // API kiểm tra giao tiếp đồng bộ giữa Payment Service -> User Service (Câu B)
    @GetMapping("/user/{userId}")
    public UserResponse getUser(@PathVariable String userId) {
        return service.getUser(userId);
    }

    @GetMapping
    public List<OrderResponse> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        return service.getAll(type, status);
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable String id) {
        return service.getById(id);
    }

    // API tạo đơn hàng liên Microservices (Câu C)
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createOrder(request));
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable String id, @Valid @RequestBody OrderStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
