package com.cinema.notification.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.cinema.notification.exception.InvalidEventException;
import com.cinema.notification.messaging.EventMessage;

@Component
public class TemplateRenderer {

    public record RenderedMessage(String subject, String body) {
    }

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy", Locale.ROOT);

    // Màu chủ đạo của từng loại thông báo
    private static final String GREEN = "#16a34a";
    private static final String RED = "#dc2626";
    private static final String BLUE = "#2563eb";

    public RenderedMessage render(EventMessage event) {
        return switch (event.eventType()) {
            case "BookingConfirmed" -> {
                Map<String, String> rows = new LinkedHashMap<>();
                rows.put("Mã đặt vé", code(event.text("bookingId")));
                rows.put("Mã suất chiếu", code(event.text("showtimeId")));
                rows.put("Số ghế", String.valueOf(event.listSize("seatIds")));
                rows.put("Xác nhận lúc", time(event.text("confirmedAt")));
                yield new RenderedMessage("Đặt vé thành công",
                        page("Đặt vé thành công", GREEN, "✓",
                                "Vé của bạn đã được xác nhận. Hẹn gặp bạn tại rạp!", rows,
                                "Vui lòng giữ lại mã đặt vé để nhận vé tại quầy."));
            }
            case "BookingCancelled" -> {
                Map<String, String> rows = new LinkedHashMap<>();
                rows.put("Mã đặt vé", code(event.text("bookingId")));
                rows.put("Lý do", reason(event.text("reasonCode")));
                rows.put("Hủy lúc", time(event.text("cancelledAt")));
                yield new RenderedMessage("Đặt vé đã bị hủy",
                        page("Đặt vé đã bị hủy", RED, "✕",
                                "Đặt vé của bạn đã bị hủy. Ghế đã được trả lại cho rạp.", rows,
                                "Nếu bạn đã thanh toán, tiền sẽ được hoàn lại và bạn sẽ nhận email xác nhận."));
            }
            case "RefundCompleted" -> {
                Map<String, String> rows = new LinkedHashMap<>();
                rows.put("Mã đặt vé", code(event.text("bookingId")));
                rows.put("Số tiền hoàn", money(event.number("amountMinor"), event.text("currency")));
                rows.put("Hoàn lúc", time(event.text("refundedAt")));
                yield new RenderedMessage("Hoàn tiền thành công",
                        page("Hoàn tiền thành công", BLUE, "₫",
                                "Chúng tôi đã hoàn tiền cho đặt vé của bạn.", rows,
                                "Thời gian tiền về tài khoản tùy thuộc vào ngân hàng của bạn."));
            }
            default -> throw new InvalidEventException("Unsupported event type");
        };
    }

    // ---------- Giao diện email (bảng + CSS inline để Gmail/Outlook hiển thị đúng) ----------

    private static String page(String title, String color, String icon, String intro,
                               Map<String, String> rows, String note) {
        StringBuilder details = new StringBuilder();
        for (Map.Entry<String, String> row : rows.entrySet()) {
            details.append("<tr>")
                    .append("<td style=\"padding:12px 0;border-bottom:1px solid #e5e7eb;color:#6b7280;font-size:14px;\">")
                    .append(esc(row.getKey())).append("</td>")
                    .append("<td align=\"right\" style=\"padding:12px 0;border-bottom:1px solid #e5e7eb;color:#111827;font-size:14px;font-weight:600;\">")
                    .append(esc(row.getValue())).append("</td></tr>");
        }
        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f3f4f6;padding:24px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;width:100%;background:#ffffff;border-radius:12px;overflow:hidden;\">"
                // Header thương hiệu
                + "<tr><td style=\"background:#111827;padding:18px 28px;color:#ffffff;font-size:18px;font-weight:700;letter-spacing:0.5px;\">🎬 Cinema Booking</td></tr>"
                // Icon + tiêu đề
                + "<tr><td align=\"center\" style=\"padding:32px 28px 8px;\">"
                + "<div style=\"width:56px;height:56px;line-height:56px;border-radius:28px;background:" + color
                + ";color:#ffffff;font-size:28px;font-weight:700;text-align:center;\">" + esc(icon) + "</div>"
                + "<h1 style=\"margin:16px 0 8px;font-size:22px;color:#111827;\">" + esc(title) + "</h1>"
                + "<p style=\"margin:0;font-size:15px;line-height:22px;color:#4b5563;\">" + esc(intro) + "</p></td></tr>"
                // Chi tiết
                + "<tr><td style=\"padding:20px 28px 8px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f9fafb;border-radius:8px;padding:4px 16px;\">"
                + "<tr><td style=\"padding:0 16px;\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + details + "</table></td></tr></table></td></tr>"
                // Ghi chú
                + "<tr><td style=\"padding:16px 28px 28px;font-size:13px;line-height:20px;color:#6b7280;\">" + esc(note) + "</td></tr>"
                // Footer
                + "<tr><td align=\"center\" style=\"padding:16px 28px;background:#f9fafb;font-size:12px;color:#9ca3af;\">"
                + "Đây là email tự động, vui lòng không trả lời.<br>© Cinema Booking</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    // ---------- Định dạng giá trị ----------

    private static String code(String id) { // UUID dài → mã ngắn dễ đọc
        String compact = id.replace("-", "").toUpperCase(Locale.ROOT);
        return "#" + (compact.length() > 8 ? compact.substring(0, 8) : compact);
    }

    private static String time(String iso) { // ISO-8601 UTC → giờ Việt Nam
        try {
            return TIME.format(Instant.parse(iso).atZone(VN));
        } catch (RuntimeException ex) {
            return iso; // không đọc được thì hiển thị nguyên văn
        }
    }

    private static String money(String amountMinor, String currency) {
        try {
            long value = Long.parseLong(amountMinor);
            String grouped = String.format(Locale.GERMANY, "%,d", value); // 150.000
            return "VND".equals(currency) ? grouped + " ₫" : grouped + " " + currency;
        } catch (NumberFormatException ex) {
            return amountMinor + " " + currency;
        }
    }

    private static String reason(String reasonCode) { // mã lý do → tiếng Việt, mã lạ giữ nguyên
        return switch (reasonCode) {
            case "USER_REQUEST" -> "Bạn yêu cầu hủy";
            case "PAYMENT_FAILED" -> "Thanh toán không thành công";
            case "PAYMENT_TIMEOUT", "HOLD_EXPIRED" -> "Hết thời gian thanh toán";
            case "SHOWTIME_CANCELLED" -> "Suất chiếu bị hủy";
            default -> reasonCode;
        };
    }

    private static String esc(String s) { // chống chèn HTML từ dữ liệu event
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}