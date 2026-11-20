package com.yt.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentServiceTest {

    @Test
    void membershipPriceAppliesLongTermDiscount() {
        assertEquals(29_000, PaymentService.membershipAmount(29_000, 1));
        assertEquals(87_000, PaymentService.membershipAmount(29_000, 3));
        // 6 thang giam 5%: 174.000 * 0,95 = 165.300 -> lam tron xuong 165.000
        assertEquals(165_000, PaymentService.membershipAmount(29_000, 6));
        // 12 thang giam 10%: 348.000 * 0,9 = 313.200 -> 313.000
        assertEquals(313_000, PaymentService.membershipAmount(29_000, 12));
    }

    @Test
    void formatsVietnameseMoney() {
        assertEquals("1.250.000đ", PaymentService.money(1_250_000));
    }
}
