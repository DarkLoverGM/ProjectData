package com.yt.backend.payment;

import com.yt.backend.entity.Order;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VnpayGatewayTest {

    private final VnpayGateway gateway = new VnpayGateway(
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "TESTCODE", "SECRET123", "http://localhost:3001");

    /** Tham so VNPay gui ve, chu ky tinh doc lap bang Python (hmac.new(secret, query, sha512)) */
    private static Map<String, String> signedReturn() {
        Map<String, String> p = new HashMap<>();
        p.put("vnp_Amount", "5000000");
        p.put("vnp_TmnCode", "TESTCODE");
        p.put("vnp_TxnRef", "order-1");
        p.put("vnp_OrderInfo", "Super Thanks cho video A");
        p.put("vnp_ResponseCode", "00");
        p.put("vnp_TransactionStatus", "00");
        p.put("vnp_TransactionNo", "14000001");
        p.put("vnp_SecureHashType", "HmacSHA512");
        p.put("vnp_SecureHash", "fb3854405f6e9a8b70540e140041843f772f49d17e48f9296eb3b61f9a33e1f0"
                + "d2cd8666283a0d3e0cfe4bc3b4aa0b3b39455e669a0eab28a442076377633581");
        return p;
    }

    private static Map<String, String> queryOf(String url) {
        Map<String, String> out = new HashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            out.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return out;
    }

    @Test
    void acceptsCorrectlySignedReturn() {
        PaymentProvider.PaymentResult r = gateway.verify(signedReturn());
        assertTrue(r.validSignature());
        assertTrue(r.success());
        assertEquals("order-1", r.orderId());
        assertEquals(50_000, r.amount()); // VNPay gui x100
        assertEquals("14000001", r.transactionId());
    }

    @Test
    void rejectsTamperedAmount() {
        Map<String, String> p = signedReturn();
        p.put("vnp_Amount", "100000");
        assertFalse(gateway.verify(p).validSignature());
    }

    @Test
    void rejectsOtherMerchant() {
        Map<String, String> p = signedReturn();
        p.put("vnp_TmnCode", "OTHER");
        assertFalse(gateway.verify(p).validSignature());
    }

    @Test
    void failedTransactionIsNotSuccess() {
        Map<String, String> p = signedReturn();
        p.put("vnp_ResponseCode", "24"); // khach huy giao dich
        // Doi field thi chu ky cu khong con dung -> phai bi tu choi chu khong duoc coi la that bai hop le
        assertFalse(gateway.verify(p).validSignature());
    }

    @Test
    void payUrlSignatureRoundTrips() {
        Order order = Order.create("order-9", "u1", Order.Type.SUPER_THANKS, "c1", "v1", null, 0, 20_000, "", "vnpay");
        String url = gateway.createPayUrl(order, "Super Thanks cho video Đẹp quá!", "0:0:0:0:0:0:0:1");
        Map<String, String> q = queryOf(url);

        assertEquals("2000000", q.get("vnp_Amount"));
        assertEquals("Super Thanks cho video Dep qua", q.get("vnp_OrderInfo"));
        assertEquals("127.0.0.1", q.get("vnp_IpAddr"));
        // Chu ky trong URL phai kiem tra lai dung bang chinh thuat toan verify
        PaymentProvider.PaymentResult r = gateway.verify(q);
        assertTrue(r.validSignature());
        assertEquals(20_000, r.amount());
        assertFalse(r.success()); // chua co ma phan hoi 00
    }

    @Test
    void asciiRemovesVietnameseMarks() {
        assertEquals("Hoi vien Fan cung kenh Dung - 3 thang", VnpayGateway.ascii("Hội viên Fan cứng kênh Dũng - 3 tháng"));
    }
}
