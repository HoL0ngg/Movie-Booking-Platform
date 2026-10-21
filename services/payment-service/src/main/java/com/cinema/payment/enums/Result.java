package com.cinema.payment.enums;

public enum Result {
    /** Đã áp dụng kết quả thành công/thất bại. */
    SETTLED,
    /** Đã ghi nhận nhưng chưa có kết quả cuối (PENDING). */
    ACKNOWLEDGED,
    /** Trùng lặp hoặc payment đã ở trạng thái cuối. */
    ALREADY_PROCESSED,
    NOT_FOUND,
    AMOUNT_MISMATCH
}
