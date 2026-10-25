import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ErrorState, LoadingState } from "../../shared/LoadingState";
import { MAX_SEATS_PER_RESERVATION, useBookingDraft } from "./BookingContext";
import { useCinemas, useSeats, useShowtime } from "./hooks";
import { useMovie } from "../movies/hooks";
import type { SeatInfo } from "./api";
import { errorMessage, money } from "./format";

export function SeatPage() {
  const { showtimeId = "" } = useParams();
  const {
    seatIds,
    isFull,
    toggleSeat,
    retainSeats,
    setMovieId,
    setShowtimeId,
  } = useBookingDraft();
  const seats = useSeats(showtimeId);
  const showtime = useShowtime(showtimeId);
  const movie = useMovie(showtime.data?.movieId);
  const cinemas = useCinemas();
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (showtime.data?.movieId) {
      setMovieId(showtime.data.movieId);
      setShowtimeId(showtimeId);
    }
  }, [showtime.data?.movieId, showtimeId, setMovieId, setShowtimeId]);

  // Sơ đồ ghế tự làm mới: nếu ghế mình đang chọn vừa bị người khác giữ/đặt thì bỏ khỏi lựa chọn và báo cho người dùng.
  useEffect(() => {
    if (!seats.data) return;
    const available = new Set(
      seats.data
        .filter((seat) => seat.status === "AVAILABLE")
        .map((seat) => seat.id),
    );
    const lost = seatIds.filter((id) => !available.has(id));
    if (!lost.length) return;
    const labels = seats.data
      .filter((seat) => lost.includes(seat.id))
      .map((seat) => seat.label)
      .join(", ");
    retainSeats((id) => available.has(id));
    setNotice(
      `Ghế ${labels} vừa có người khác giữ hoặc đặt nên đã được bỏ khỏi lựa chọn của bạn.`,
    );
  }, [seats.data, seatIds, retainSeats]);

  const page = (content: React.ReactNode) => (
    <div className="page-width page-top">{content}</div>
  );

  if (showtime.isError)
    return page(<ErrorState message={errorMessage(showtime.error)} />);
  if (seats.isLoading || movie.isLoading)
    return page(<LoadingState label="Đang tải sơ đồ ghế…" />);
  if (seats.isError) {
    return page(
      <>
        <ErrorState
          message={errorMessage(seats.error, "Không thể tải sơ đồ ghế.")}
        />
        <button
          className="secondary-button"
          onClick={() => void seats.refetch()}
        >
          Thử lại
        </button>
      </>,
    );
  }
  if (!seats.data?.length)
    return page(<ErrorState message="Suất chiếu này chưa có sơ đồ ghế." />);

  const selected = seats.data.filter((seat) => seatIds.includes(seat.id));
  const total = selected.reduce((sum, seat) => sum + seat.price.amountMinor, 0);
  const rows = Object.entries(
    seats.data.reduce<Record<string, SeatInfo[]>>((grouped, seat) => {
      (grouped[seat.row] ??= []).push(seat);
      return grouped;
    }, {}),
  );
  const selectedShowtime = showtime.data;
  const cinema = cinemas.data?.find(
    (item) => item.id === selectedShowtime?.cinemaId,
  );
  const movieId = movie.data?.id ?? selectedShowtime?.movieId;

  return (
    <div className="page-width page-top seat-page">
      <div className="booking-heading">
        <div>
          <Link
            className="back-link"
            to={movieId ? `/showtimes?movieId=${movieId}` : "/showtimes"}
          >
            ← Đổi suất chiếu
          </Link>
          <p className="eyebrow">Chọn ghế</p>
          <h1>{movie.data?.title}</h1>
          <p>
            {cinema?.name ?? "Cinémat"} ·{" "}
            {selectedShowtime
              ? new Intl.DateTimeFormat("vi-VN", {
                  weekday: "long",
                  day: "2-digit",
                  month: "2-digit",
                  hour: "2-digit",
                  minute: "2-digit",
                }).format(new Date(selectedShowtime.startsAt))
              : "Suất chiếu đã chọn"}
          </p>
        </div>
        <div className="stepper">
          <span className="done">1</span>
          <i />
          <span className="active">2</span>
          <i />
          <span>3</span>
        </div>
      </div>

      {notice && (
        <p className="form-error" role="status">
          {notice}
        </p>
      )}

      <div className="seat-layout-card">
        <div className="screen">
          <span>Màn hình</span>
        </div>
        <div className="seat-map" role="group" aria-label="Sơ đồ ghế">
          {rows.map(([row, rowSeats]) => (
            <div className="seat-row" key={row}>
              <b>{row}</b>
              <div>
                {rowSeats.map((seat) => {
                  const unavailable = seat.status !== "AVAILABLE";
                  const active = seatIds.includes(seat.id);
                  const blockedByLimit = !active && isFull;
                  return (
                    <button
                      key={seat.id}
                      className={`seat ${seat.type.toLowerCase()} ${active ? "selected" : ""} ${unavailable ? "unavailable" : ""}`}
                      disabled={unavailable || blockedByLimit}
                      aria-pressed={active}
                      aria-label={`${seat.label}, ghế ${seat.type === "VIP" ? "VIP" : "thường"}, ${unavailable ? "không khả dụng" : active ? "đã chọn" : "còn trống"}`}
                      onClick={() => {
                        setNotice("");
                        toggleSeat(seat.id);
                      }}
                    >
                      <span>{seat.number || seat.label}</span>
                    </button>
                  );
                })}
              </div>
              <b>{row}</b>
            </div>
          ))}
        </div>
        <div className="seat-legend">
          <span>
            <i className="seat-demo" />
            Còn trống
          </span>
          <span>
            <i className="seat-demo selected" />
            Đang chọn
          </span>
          <span>
            <i className="seat-demo unavailable" />
            Đã đặt / đang giữ
          </span>
          <span>
            <i className="seat-demo vip" />
            VIP
          </span>
        </div>
        {isFull && (
          <p className="muted" role="status">
            Bạn chỉ có thể chọn tối đa {MAX_SEATS_PER_RESERVATION} ghế mỗi lần
            đặt.
          </p>
        )}
      </div>

      <aside className="booking-bar">
        <div>
          <small>Ghế đã chọn</small>
          <strong>
            {selected.length
              ? selected.map((seat) => seat.label).join(", ")
              : "Chưa chọn ghế"}
          </strong>
        </div>
        <div>
          <small>Tổng cộng</small>
          <strong className="price">{money(total)}</strong>
        </div>
        {selected.length ? (
          <Link className="primary-button" to="/checkout">
            Tiếp tục →
          </Link>
        ) : (
          <button className="primary-button" disabled>
            Chọn ghế để tiếp tục
          </button>
        )}
      </aside>
    </div>
  );
}
