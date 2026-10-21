package com.cinema.payment.enums;

public enum ProviderOutcome

{
    SUCCEEDED,
    /**
     * Chưa có kết quả cuối cùng (chưa hoàn tất / giao dịch đảo / nghi ngờ gian
     * lận). Không đổi trạng thái.
     */
    PENDING,
    FAILED
}