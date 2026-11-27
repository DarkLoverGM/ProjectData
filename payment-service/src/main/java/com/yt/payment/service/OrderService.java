package com.yt.payment.service;

import com.yt.payment.client.UserClient;
import com.yt.payment.client.VideoClient;
import com.yt.payment.dto.*;
import com.yt.payment.entity.MembershipTier;
import com.yt.payment.entity.Order;
import com.yt.payment.exception.BadRequestException;
import com.yt.payment.exception.ResourceNotFoundException;
import com.yt.payment.repository.MembershipTierRepository;
import com.yt.payment.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MembershipTierRepository tierRepository;
    private final VideoClient videoClient;
    private final UserClient userClient;

    public OrderService(
            OrderRepository orderRepository,
            MembershipTierRepository tierRepository,
            VideoClient videoClient,
            UserClient userClient
    ) {
        this.orderRepository = orderRepository;
        this.tierRepository = tierRepository;
        this.videoClient = videoClient;
        this.userClient = userClient;
    }

    // Kiểm tra giao tiếp đồng bộ sang Video Service (Câu B)
    public VideoResponse getVideo(String videoId) {
        return videoClient.getById(videoId);
    }

    // Kiểm tra giao tiếp đồng bộ sang User Service (Câu B)
    public UserResponse getUser(String userId) {
        return userClient.getById(userId);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getUserName(),
                order.getType(),
                order.getChannelId(),
                order.getChannelName(),
                order.getVideoId(),
                order.getVideoTitle(),
                order.getTierId(),
                order.getTierName(),
                order.getMonths(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getMessage(),
                order.getProvider(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    public List<OrderResponse> getAll(String type, String status) {
        List<Order> orders;
        if (type != null && !type.isBlank()) {
            orders = orderRepository.findByTypeIgnoreCase(type.trim());
        } else if (status != null && !status.isBlank()) {
            orders = orderRepository.findByStatusIgnoreCase(status.trim());
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream().map(this::toResponse).toList();
    }

    public OrderResponse getById(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id));
        return toResponse(order);
    }

    /**
     * Quy trình nghiệp vụ liên Microservices (Câu C):
     * - Gọi User Service kiểm tra người mua.
     * - Nếu MEMBERSHIP: kiểm tra gói hội viên, gọi User Service lấy thông tin kênh, tính tổng tiền = đơn giá * số tháng.
     * - Nếu SUPER_THANKS: gọi Video Service lấy thông tin video & kênh, gọi User Service lấy tên kênh, lưu đơn ủng hộ.
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        UserResponse buyer = userClient.getById(request.userId().trim());

        String type = request.type().toUpperCase();
        String orderId = "ord-" + UUID.randomUUID().toString().substring(0, 8);
        String msg = request.message() == null ? "" : request.message().trim();

        Order order;
        if ("MEMBERSHIP".equals(type)) {
            if (request.tierId() == null || request.tierId().isBlank()) {
                throw new BadRequestException("Vui lòng chọn gói hội viên (tierId)");
            }
            int months = (request.months() == null) ? 1 : request.months();
            if (months < 1) {
                throw new BadRequestException("Số tháng đăng ký phải lớn hơn 0");
            }

            MembershipTier tier = tierRepository.findById(request.tierId().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói hội viên: " + request.tierId()));

            if (!tier.isActive()) {
                throw new BadRequestException("Gói hội viên hiện đang tạm ngưng: " + tier.getName());
            }

            UserResponse channel = userClient.getById(tier.getChannelId());
            BigDecimal totalAmount = tier.getPrice().multiply(BigDecimal.valueOf(months));

            order = new Order(
                    orderId,
                    buyer.id(),
                    buyer.channelName(),
                    "MEMBERSHIP",
                    channel.id(),
                    channel.channelName(),
                    null,
                    null,
                    tier.getId(),
                    tier.getName(),
                    months,
                    tier.getPrice(),
                    totalAmount,
                    msg,
                    request.provider().toUpperCase(),
                    "PAID",
                    Instant.now()
            );
        } else {
            // SUPER_THANKS
            if (request.videoId() == null || request.videoId().isBlank()) {
                throw new BadRequestException("Vui lòng chọn video để gửi Super Thanks (videoId)");
            }
            if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Số tiền ủng hộ Super Thanks phải lớn hơn 0");
            }

            VideoResponse video = videoClient.getById(request.videoId().trim());
            UserResponse channel = userClient.getById(video.channelId());

            order = new Order(
                    orderId,
                    buyer.id(),
                    buyer.channelName(),
                    "SUPER_THANKS",
                    channel.id(),
                    channel.channelName(),
                    video.id(),
                    video.title(),
                    null,
                    null,
                    0,
                    request.amount(),
                    request.amount(),
                    msg,
                    request.provider().toUpperCase(),
                    "PAID",
                    Instant.now()
            );
        }

        return toResponse(orderRepository.save(order));
    }

    public OrderResponse updateStatus(String id, OrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id));
        order.setStatus(request.status().toUpperCase());
        return toResponse(orderRepository.save(order));
    }

    public void delete(String id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id);
        }
        orderRepository.deleteById(id);
    }
}
