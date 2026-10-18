import { Link } from "react-router-dom";
import { ErrorState, LoadingState } from "../../shared/LoadingState";
import { BOOKING_STATUS_LABEL, type Booking } from "./api";
import { useBookingHistory, useShowtimeContext } from "./hooks";
import { dateTime, errorMessage } from "./format";

// Mỗi thẻ tự lấy thông tin suất/phim/rạp (React Query cache nên các vé cùng suất chỉ gọi một lần).
function BookingCard({ booking }: { booking: Booking }) {
  const { movie, cinema, showtime, showtimeError } = useShowtimeContext(
    booking.showtimeId,
  );
  return (
    <article className="history-card">
      <img src={movie?.posterUrl} alt="" />
      <div className="history-info">
        <div>
          <span className={`status ${booking.status.toLowerCase()}`}>
            {BOOKING_STATUS_LABEL[booking.status] ?? booking.status}
          </span>
          <h2>
            {movie?.title ??
              (showtimeError ? "Suất chiếu không còn khả dụng" : "Đang tải…")}
          </h2>
          <p>{cinema?.name}</p>
        </div>
        <dl>
          <div>
            <dt>Suất chiếu</dt>
            <dd>{showtime ? dateTime(showtime.startsAt) : "—"}</dd>
          </div>
          <div>
            <dt>Ghế</dt>
            <dd>{booking.seats.map((seat) => seat.seatLabel).join(", ")}</dd>
          </div>
          <div>
            <dt>Mã vé</dt>
            <dd>{booking.bookingCode}</dd>
          </div>
        </dl>
        {booking.status === "CONFIRMED" && (
          <Link to={`/booking-success/${booking.id}`}>Xem vé →</Link>
        )}
      </div>
    </article>
  );
}

export function HistoryPage() {
  const bookings = useBookingHistory();
  return (
    <div className="page-width page-top history-page">
      <div className="page-title">
        <p className="eyebrow">Tài khoản</p>
        <h1>Vé của tôi</h1>
        <p>Tất cả hành trình điện ảnh ở một nơi.</p>
      </div>
      {bookings.isLoading ? (
        <LoadingState />
      ) : bookings.isError ? (
        <>
          <ErrorState
            message={errorMessage(
              bookings.error,
              "Không thể tải danh sách vé.",
            )}
          />
          <button
            className="secondary-button"
            onClick={() => void bookings.refetch()}
          >
            Thử lại
          </button>
        </>
      ) : !bookings.data?.length ? (
        <p role="status">
          Bạn chưa có vé nào. <Link to="/showtimes">Đặt vé ngay →</Link>
        </p>
      ) : (
        <div className="history-list">
          {bookings.data.map((booking) => (
            <BookingCard key={booking.id} booking={booking} />
          ))}
        </div>
      )}
    </div>
  );
}
