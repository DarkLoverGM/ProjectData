package com.yt.backend.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.common.Json;
import com.yt.backend.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Vi MoMo, API v2 "One-time payment" (https://developers.momo.vn/v3/docs/payment/api/wallet/onetime).
 * requestType payWithMethod: trang MoMo cho chon vi / the ATM / the quoc te, test duoc tren trinh duyet
 * bang the ATM test ma khong can cai app.
 * Chu ky: HmacSHA256 tren chuoi key=value (thu tu key a-z) theo dung danh sach field tai lieu MoMo quy dinh.
 */
@Component
public class MomoGateway implements PaymentProvider {

    private static final String REQUEST_TYPE = "payWithMethod";

    private final String endpoint;
    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final String serverUrl;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public MomoGateway(@Value("${app.payment.momo.endpoint}") String endpoint,
                       @Value("${app.payment.momo.partner-code}") String partnerCode,
                       @Value("${app.payment.momo.access-key}") String accessKey,
                       @Value("${app.payment.momo.secret-key}") String secretKey,
                       @Value("${app.server-url}") String serverUrl,
                       ObjectMapper mapper) {
        this.endpoint = endpoint;
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.serverUrl = serverUrl;
        this.mapper = mapper;
    }

    @Override
    public String id() {
        return "momo";
    }

    @Override
    public String label() {
        return "Ví MoMo";
    }

    @Override
    public boolean isConfigured() {
        return !endpoint.isBlank() && !partnerCode.isBlank() && !accessKey.isBlank() && !secretKey.isBlank();
    }

    @Override
    public String createPayUrl(Order order, String description, String clientIp) throws IOException {
        String redirectUrl = serverUrl + "/api/payments/momo/return";
        String ipnUrl = serverUrl + "/api/payments/momo/ipn";
        String extraData = "";
        // requestId = id don: gui lai cung 1 don thi MoMo coi la trung, khong tao 2 giao dich
        String raw = "accessKey=" + accessKey
                + "&amount=" + order.amountValue()
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + order.getId()
                + "&orderInfo=" + description
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + order.getId()
                + "&requestType=" + REQUEST_TYPE;

        Map<String, Object> body = Json.of(
                "partnerCode", partnerCode,
                "partnerName", "hello-react",
                "storeId", "hello-react",
                "requestId", order.getId(),
                "amount", order.amountValue(),
                "orderId", order.getId(),
                "orderInfo", description,
                "redirectUrl", redirectUrl,
                "ipnUrl", ipnUrl,
                "lang", "vi",
                "requestType", REQUEST_TYPE,
                "autoCapture", true,
                "extraData", extraData,
                "signature", Hmac.sha256(secretKey, raw));

        HttpResponse<String> res;
        try {
            res = http.send(HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }

        JsonNode data = mapper.readTree(res.body());
        String payUrl = data.path("payUrl").asText("");
        if (data.path("resultCode").asInt(-1) != 0 || payUrl.isEmpty()) {
            throw new IOException("MoMo từ chối tạo giao dịch: " + data.path("message").asText(res.body()));
        }
        return payUrl;
    }

    @Override
    public PaymentResult verify(Map<String, String> p) {
        if (!partnerCode.equals(p.get("partnerCode"))) return PaymentResult.invalid();
        String raw = "accessKey=" + accessKey
                + "&amount=" + v(p, "amount")
                + "&extraData=" + v(p, "extraData")
                + "&message=" + v(p, "message")
                + "&orderId=" + v(p, "orderId")
                + "&orderInfo=" + v(p, "orderInfo")
                + "&orderType=" + v(p, "orderType")
                + "&partnerCode=" + v(p, "partnerCode")
                + "&payType=" + v(p, "payType")
                + "&requestId=" + v(p, "requestId")
                + "&responseTime=" + v(p, "responseTime")
                + "&resultCode=" + v(p, "resultCode")
                + "&transId=" + v(p, "transId");
        if (!Hmac.matches(Hmac.sha256(secretKey, raw), p.get("signature"))) return PaymentResult.invalid();

        long amount;
        try {
            amount = Long.parseLong(v(p, "amount"));
        } catch (NumberFormatException e) {
            return PaymentResult.invalid();
        }
        // resultCode 0 = thanh cong, 9000 = da uy quyen (autoCapture=true nen khong gap), con lai la that bai / huy
        return new PaymentResult(true, v(p, "orderId"), "0".equals(v(p, "resultCode")), amount,
                v(p, "transId"), v(p, "message"));
    }

    private static String v(Map<String, String> p, String key) {
        String value = p.get(key);
        return value == null ? "" : value;
    }
}
