package com.yt.backend.payment;

import com.yt.backend.entity.Order;

import java.io.IOException;
import java.util.Map;

/**
 * 1 cong thanh toan online (MoMo, VNPay...). PaymentService nhan tat ca cai dat qua Spring
 * (List&lt;PaymentProvider&gt;) nen them cong moi chi can viet them 1 @Component, khong phai sua service.
 *
 * Luong thanh toan:
 *  1. createPayUrl: tao link thanh toan cho don, frontend chuyen nguoi dung sang do
 *  2. Nguoi dung tra tien xong, cong chuyen trinh duyet ve return URL va (rieng) goi IPN toi server
 *  3. verify: kiem tra chu ky cua tham so cong gui ve, doc ket qua -> PaymentService cap nhat don
 */
public interface PaymentProvider {

    /** Ma cong, luu vao orders.provider va nam trong duong dan callback: "momo" | "vnpay" */
    String id();

    /** Ten hien thi tren giao dien */
    String label();

    /** Du key de goi cong chua (thieu thi giao dien an lua chon nay) */
    boolean isConfigured();

    /**
     * @param description noi dung don hien tren trang thanh toan
     * @param clientIp    IP nguoi mua (VNPay bat buoc)
     * @return URL trang thanh toan cua cong
     * @throws IOException khi khong goi duoc cong / cong tu choi tao giao dich
     */
    String createPayUrl(Order order, String description, String clientIp) throws IOException;

    /** Kiem tra chu ky + doc ket qua tu tham so cong gui ve (return URL hoac IPN) */
    PaymentResult verify(Map<String, String> params);

    /**
     * Ket qua cong bao ve.
     * @param validSignature chu ky dung (sai thi bo qua moi field khac)
     * @param success        cong bao da tru tien thanh cong
     * @param amount         so tien (VND) cong xac nhan, phai khop voi don
     */
    record PaymentResult(boolean validSignature, String orderId, boolean success, long amount,
                         String transactionId, String message) {

        static PaymentResult invalid() {
            return new PaymentResult(false, null, false, 0, null, "Sai chữ ký");
        }
    }
}
