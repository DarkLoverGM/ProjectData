package com.yt.payment.repository;

import com.yt.payment.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByTypeIgnoreCase(String type);
    List<Order> findByStatusIgnoreCase(String status);
    List<Order> findByUserId(String userId);
}
