package com.yt.backend.repository;

import com.yt.backend.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Don thanh toan. */
public interface OrderRepository extends MongoRepository<Order, String> {

    Optional<Order> findOneById(String id);

    Page<Order> findByUserId(String userId, Pageable pageable);

    List<Order> findByChannelIdAndStatus(String channelId, String status, Sort sort);

    List<Order> findByStatus(String status);

    long countByStatus(String status);

    long countByUserIdAndStatusAndCreatedAtAfter(String userId, String status, Date after);

    long countByStatusAndCreatedAtGreaterThanEqual(String status, Date after);
}
