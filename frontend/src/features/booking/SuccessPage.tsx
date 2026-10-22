import { Link, useParams } from "react-router-dom";
import { ErrorState, LoadingState } from "../../shared/LoadingState";
import { BOOKING_STATUS_LABEL } from "./api";
import { useBooking, useShowtimeContext } from "./hooks";
import { dateTime, errorMessage } from "./format";

export function SuccessPage() {
  const { bookingId = "" } = useParams();
  const booking = useBooking(bookingId);
  const details = useShowtimeContext(booking.data?.showtimeId);

  if (booking.isLoading)
    return (
      <div className="page-width page-top">
        <LoadingState label="Đang xác minh booking…" />
      </div>
    );
  if (booking.isError || !booking.data) {
    return (
      <div className="page-width page-top">
        <ErrorState
          message={errorMessage(booking.error, "Không tìm thấy booking.")}
        />
        <Link className="secondary-button" to="/bookings">
          Xem vé của tôi
        </Link>
      </div>
    );
  }

  const item = booking.data;
  const confirmed = item.status === "CONFIRMED";
  return (
    <div className="success-page page-width page-top">
      <div className="success-icon">{confirmed ? "✓" : "…"}</div>
      <p className="eyebrow">
        {confirmed
          ? "Thanh toán thành công"
          : (BOOKING_STATUS_LABEL[item.status] ?? item.status)}
      </p>
      <h1>
        {confirmed ? "Hẹn bạn tại rạp!" : "Vé của bạn chưa được xác nhận"}
      </h1>
      <p>
        {confirmed
          ? "Booking đã được xác nhận."
          : "Bạn có thể theo dõi trạng thái trong mục Vé của tôi."}
      </p>
      <article className="ticket">
        <div className="ticket-main">
          <span className="ticket-brand">cinémat</span>
          <h2>{details.movie?.title ?? "Đang tải…"}</h2>
          <p>{details.cinema?.name}</p>
          <div className="ticket-data">
            <div>
              <small>Ngày & giờ</small>
              <strong>
                {details.showtime ? dateTime(details.showtime.startsAt) : "—"}
              </strong>
            </div>
            <div>
              <small>Ghế</small>
              <strong>
                {item.seats.map((seat) => seat.seatLabel).join(", ")}
              </strong>
            </div>
            <div>
              <small>Định dạng</small>
              <strong>{details.showtime?.format ?? "—"}</strong>
            </div>
          </div>
        </div>
        <div className="ticket-stub">
          <div className="qr-placeholder" aria-label="Mã QR vé">
            ▦
          </div>
          <small>Mã đặt vé</small>
          <strong>{item.bookingCode}</strong>
        </div>
      </article>
      <div className="button-row centered">
        <Link className="primary-button" to="/bookings">
          Xem vé của tôi
        </Link>
        <Link className="secondary-button" to="/">
          Về trang chủ
        </Link>
      </div>
    </div>
  );
}
