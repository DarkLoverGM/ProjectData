package com.yt.backend.payment;

import com.yt.backend.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * VNPay, cong thanh toan phien ban 2.1.0 (https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html).
 * Khong goi API: tao URL vpcpay.html kem tham so + chu ky, nguoi dung chon ngan hang / the / QR tren trang VNPay.
 * Chu ky: HmacSHA512 tren chuoi query (key sap xep a-z, value da URL-encode) - giong code mau Java cua VNPay.
 * Key sandbox (TMN code + hash secret) dang ky mien phi tai https://sandbox.vnpayment.vn/devreg/
 */
@Component
public class VnpayGateway implements PaymentProvider {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final String payUrl;
    private final String tmnCode;
    private final String hashSecret;
    private final String serverUrl;

    public VnpayGateway(@Value("${app.payment.vnpay.url}") String payUrl,
                        @Value("${app.payment.vnpay.tmn-code}") String tmnCode,
                        @Value("${app.payment.vnpay.hash-secret}") String hashSecret,
                        @Value("${app.server-url}") String serverUrl) {
        this.payUrl = payUrl;
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
        this.serverUrl = serverUrl;
    }

    @Override
    public String id() {
        return "vnpay";
    }

    @Override
    public String label() {
        return "VNPay (ATM / Visa / QR)";
    }

    @Override
    public boolean isConfigured() {
        return !payUrl.isBlank() && !tmnCode.isBlank() && !hashSecret.isBlank();
    }

    @Override
    public String createPayUrl(Order order, String description, String clientIp) {
        ZonedDateTime now = ZonedDateTime.now(VN);
        SortedMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(order.amountValue() * 100)); // VNPay tinh bang don vi 1/100 dong
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", order.getId());
        params.put("vnp_OrderInfo", ascii(description));
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", serverUrl + "/api/payments/vnpay/return");
        params.put("vnp_IpAddr", ipv4(clientIp));
        params.put("vnp_CreateDate", VNP_DATE.format(now));
        params.put("vnp_ExpireDate", VNP_DATE.format(now.plusMinutes(Order.EXPIRES_MINUTES)));

        String query = encode(params);
        return payUrl + "?" + query + "&vnp_SecureHash=" + Hmac.sha512(hashSecret, query);
    }

    @Override
    public PaymentResult verify(Map<String, String> p) {
        if (!tmnCode.equals(p.get("vnp_TmnCode"))) return PaymentResult.invalid();
        // Chu ky tinh tren moi tham so vnp_* co gia tri, tru chinh 2 field chu ky
        SortedMap<String, String> fields = new TreeMap<>();
        p.forEach((k, v) -> {
            if (k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType")
                    && v != null && !v.isEmpty()) {
                fields.put(k, v);
            }
        });
        if (!Hmac.matches(Hmac.sha512(hashSecret, encode(fields)), p.get("vnp_SecureHash"))) {
            return PaymentResult.invalid();
        }

        long amount;
        try {
            amount = Long.parseLong(p.getOrDefault("vnp_Amount", "")) / 100;
        } catch (NumberFormatException e) {
            return PaymentResult.invalid();
        }
        // Ca ma phan hoi lan trang thai giao dich deu "00" moi la da tru tien
        boolean success = "00".equals(p.get("vnp_ResponseCode")) && "00".equals(p.get("vnp_TransactionStatus"));
        return new PaymentResult(true, p.get("vnp_TxnRef"), success, amount, p.get("vnp_TransactionNo"),
                success ? "Thành công" : "Mã lỗi VNPay " + p.get("vnp_ResponseCode"));
    }

    /** key=value&... voi value URL-encode (dau cach thanh +), dung cho ca query lan chuoi ky */
    private static String encode(SortedMap<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /** VNPay yeu cau vnp_OrderInfo tieng Viet khong dau, khong ky tu dac biet */
    static String ascii(String text) {
        String noMarks = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        String cleaned = noMarks.replaceAll("[^A-Za-z0-9 .,:-]", "").trim();
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    /** IP loopback IPv6 (chay local) doi ve 127.0.0.1 cho chac VNPay nhan */
    private static String ipv4(String ip) {
        if (ip == null || ip.isEmpty() || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) return "127.0.0.1";
        return ip.startsWith("::ffff:") ? ip.substring(7) : ip;
    }
}
