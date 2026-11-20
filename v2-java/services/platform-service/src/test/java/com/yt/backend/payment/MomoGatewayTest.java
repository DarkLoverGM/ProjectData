package com.yt.backend.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MomoGatewayTest {

    private final MomoGateway gateway = new MomoGateway("https://test-payment.momo.vn/v2/gateway/api/create",
            "MOMO", "ACCESS", "SECRETKEY", "http://localhost:3001", new ObjectMapper());

    /** Ket qua MoMo gui ve, chu ky tinh doc lap bang Python (hmac.new(secret, raw, sha256)) */
    private static Map<String, String> signedResult() {
        Map<String, String> p = new HashMap<>();
        p.put("partnerCode", "MOMO");
        p.put("orderId", "order-2");
        p.put("requestId", "order-2");
        p.put("amount", "50000");
        p.put("orderInfo", "test");
        p.put("orderType", "momo_wallet");
        p.put("transId", "4999");
        p.put("resultCode", "0");
        p.put("message", "Successful.");
        p.put("payType", "qr");
        p.put("responseTime", "1730000000001");
        p.put("extraData", "");
        p.put("signature", "7f469079813601de5084d85c38a21aab62cf3a2e410fbfae2aad5b8d50a8e3f1");
        return p;
    }

    @Test
    void acceptsCorrectlySignedResult() {
        PaymentProvider.PaymentResult r = gateway.verify(signedResult());
        assertTrue(r.validSignature());
        assertTrue(r.success());
        assertEquals("order-2", r.orderId());
        assertEquals(50_000, r.amount());
        assertEquals("4999", r.transactionId());
    }

    @Test
    void rejectsForgedSuccess() {
        Map<String, String> p = signedResult();
        p.put("amount", "1000");
        assertFalse(gateway.verify(p).validSignature());

        p = signedResult();
        p.put("signature", "00" + p.get("signature").substring(2));
        assertFalse(gateway.verify(p).validSignature());
    }

    @Test
    void rejectsMissingSignature() {
        Map<String, String> p = signedResult();
        p.remove("signature");
        assertFalse(gateway.verify(p).validSignature());
    }
}
