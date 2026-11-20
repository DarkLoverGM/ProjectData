package com.yt.backend.payment;

import com.yt.backend.entity.Order;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private static Order pendingCreatedMinutesAgo(int minutes) {
        Order order = Order.create("o1", "u1", Order.Type.MEMBERSHIP, "c1", null, "t1", 1, 29_000, null, "momo");
        order.setCreatedAt(new Date(System.currentTimeMillis() - minutes * 60_000L));
        return order;
    }

    @Test
    void pendingOrderExpiresAfterTimeout() {
        assertEquals(Order.Status.PENDING, pendingCreatedMinutesAgo(1).effectiveStatus());
        assertEquals(Order.Status.EXPIRED, pendingCreatedMinutesAgo(Order.EXPIRES_MINUTES + 1).effectiveStatus());
    }

    @Test
    void paidOrderNeverExpires() {
        Order order = pendingCreatedMinutesAgo(Order.EXPIRES_MINUTES + 60);
        order.setStatus(Order.Status.PAID.code());
        assertEquals(Order.Status.PAID, order.effectiveStatus());
    }

    @Test
    void storesLowercaseCodes() {
        Order order = Order.create("o2", "u1", Order.Type.SUPER_THANKS, "c1", "v1", null, 0, 50_000, "Hay!", "vnpay");
        assertEquals("super_thanks", order.getType());
        assertEquals("pending", order.getStatus());
        assertEquals(Order.Type.SUPER_THANKS, order.typeEnum());
    }

    @Test
    void parsesCodes() {
        assertEquals(Order.Type.MEMBERSHIP, Order.Type.of("membership"));
        assertNull(Order.Type.of("MEMBERSHIP"));
        assertNull(Order.Status.of("refunded"));
    }
}
