package com.yt.backend.controller;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ClientIp;
import com.yt.backend.dto.Paging;
import com.yt.backend.dto.payment.*;
import com.yt.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Thanh toan online (hoi vien kenh, Super Thanks):
 *   GET  /api/payments/config         bang gia + cac cong dang dung duoc
 *   POST /api/payments/checkout       tao don, tra payUrl de frontend chuyen sang cong
 *   GET  /api/payments/orders[/{id}]  lich su / chi tiet don cua toi
 *   GET  /api/payments/{momo|vnpay}/return  cong chuyen trinh duyet ve -> cap nhat don -> ve /payment/result
 *   POST /api/payments/momo/ipn, GET /api/payments/vnpay/ipn  cong goi thang server bao ket qua
 *   GET  /api/admin/payments          admin xem moi giao dich
 */
@RestController
@RequestMapping("/api")
public class PaymentController {

    /** Tra loi IPN cho VNPay (VNPay doc RspCode, khong phai 00/02 thi goi lai) */
    public record VnpayIpnResponse(String RspCode, String Message) {}

    private final PaymentService paymentService;
    private final String clientUrl;

    public PaymentController(PaymentService paymentService, @Value("${app.client-url}") String clientUrl) {
        this.paymentService = paymentService;
        this.clientUrl = clientUrl;
    }

    /** Tham so query (return URL, IPN cua VNPay), moi key lay gia tri dau */
    private static Map<String, String> params(HttpServletRequest req) {
        Map<String, String> out = new HashMap<>();
        req.getParameterMap().forEach((k, v) -> out.put(k, v.length > 0 ? v[0] : ""));
        return out;
    }

    @GetMapping("/payments/config")
    public PricingResponse config() {
        return paymentService.pricing();
    }

    @PostMapping("/payments/checkout")
    @PreAuthorize("isAuthenticated()")
    public CheckoutResponse checkout(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CheckoutRequest body,
                                     HttpServletRequest req) {
        return paymentService.checkout(user, body, ClientIp.of(req));
    }

    @GetMapping("/payments/orders")
    @PreAuthorize("isAuthenticated()")
    public MyOrdersResponse myOrders(@AuthenticationPrincipal AuthUser user, @RequestParam(required = false) String page,
                                     @RequestParam(required = false) String limit) {
        return paymentService.myOrders(user.id(), Paging.of(page, limit));
    }

    @GetMapping("/payments/orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public OrderDto order(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        return paymentService.getOrder(user, id);
    }

    // ================= Cong thanh toan goi ve (khong can dang nhap, xac thuc bang chu ky) =================

    /** Ve trang ket qua tren frontend, trang do tu hoi lai trang thai don */
    private void toResultPage(HttpServletResponse res, PaymentService.CallbackResult result) throws IOException {
        String query = result.orderId() == null
                ? "error=" + result.outcome().name().toLowerCase()
                : "orderId=" + URLEncoder.encode(result.orderId(), StandardCharsets.UTF_8);
        res.sendRedirect(clientUrl + "/payment/result?" + query);
    }

    @GetMapping("/payments/momo/return")
    public void momoReturn(HttpServletRequest req, HttpServletResponse res) throws IOException {
        toResultPage(res, paymentService.handleCallback("momo", params(req)));
    }

    /** MoMo POST JSON, chi can tra 204 (https://developers.momo.vn/v3/docs/payment/api/result-handling/notification) */
    @PostMapping("/payments/momo/ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody(required = false) Map<String, Object> body) {
        // Body cua cong ben ngoai: doc thang thanh Map vi chu ky tinh tren dung cac field MoMo gui
        Map<String, String> p = new HashMap<>();
        if (body != null) body.forEach((k, v) -> p.put(k, v == null ? "" : String.valueOf(v)));
        paymentService.handleCallback("momo", p);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/payments/vnpay/return")
    public void vnpayReturn(HttpServletRequest req, HttpServletResponse res) throws IOException {
        toResultPage(res, paymentService.handleCallback("vnpay", params(req)));
    }

    @GetMapping("/payments/vnpay/ipn")
    public VnpayIpnResponse vnpayIpn(HttpServletRequest req) {
        return switch (paymentService.handleCallback("vnpay", params(req)).outcome()) {
            case CONFIRMED -> new VnpayIpnResponse("00", "Confirm Success");
            case ALREADY_CONFIRMED -> new VnpayIpnResponse("02", "Order already confirmed");
            case ORDER_NOT_FOUND -> new VnpayIpnResponse("01", "Order not found");
            case AMOUNT_MISMATCH -> new VnpayIpnResponse("04", "Invalid amount");
            case INVALID_SIGNATURE -> new VnpayIpnResponse("97", "Invalid signature");
        };
    }

    // ================= Admin (/api/admin/** chi role ADMIN, xem SecurityConfig) =================

    @GetMapping("/admin/payments")
    public AdminPaymentsResponse adminList(@RequestParam(defaultValue = "") String search,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String provider,
                                           @RequestParam(required = false) String type,
                                           @RequestParam(required = false) String page,
                                           @RequestParam(required = false) String limit) {
        return paymentService.adminList(search, status, provider, type, Paging.of(page, limit));
    }
}
