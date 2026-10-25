package com.cinema.payment.provider;

/**
 * Hợp đồng chung cho một phương thức/cổng thanh toán. {@code PaymentService}
 * chỉ làm việc với interface này.
 *
 * <p>
 * Thêm phương thức mới (MoMo, ZaloPay, Stripe...): viết một {@code @Component}
 * implement interface này
 * với {@link #code()} duy nhất; {@link PaymentProviderRegistry} tự phát hiện.
 * Client chọn bằng trường
 * {@code provider} trong request tạo thanh toán, hoặc dùng
 * {@code payment.default-provider}.
 *
 * <p>
 * Lưu ý: hiện interface mới bao phủ bước tạo checkout. Xử lý thông báo
 * (IPN/webhook), đối soát và hoàn tiền
 * vẫn do các class riêng của từng provider đảm nhiệm (xem README, mục "Mở
 * rộng").
 */
public interface PaymentProvider {

    /**
     * Mã định danh ổn định, chữ thường; được lưu ở payment_attempts.provider. Ví
     * dụ: "vnpay".
     */
    String code();

    /**
     * Giới hạn và chính sách thời gian của provider; PaymentService dùng để kiểm
     * tra trước khi tạo checkout.
     */
    CheckoutRules rules();

    /**
     * Tạo phiên thanh toán và trả về địa chỉ để chuyển hướng khách. Không được thực
     * hiện I/O dài hoặc ghi DB:
     * được gọi bên trong transaction tạo payment.
     *
     * @throws IllegalArgumentException khi {@code request.options()} chứa giá trị
     *                                  provider không chấp nhận
     */
    CheckoutSession createCheckout(CheckoutRequest request);
}
