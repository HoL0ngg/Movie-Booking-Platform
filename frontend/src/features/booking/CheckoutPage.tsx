import { Navigate } from "react-router-dom";
import { useBookingDraft } from "./BookingContext";
import { useCheckout, useCinemas, useSeats, useShowtime } from "./hooks";
import { useMovie } from "../movies/hooks";

const money = (value: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    value,
  );

export function CheckoutPage() {
  const draft = useBookingDraft();
  const checkout = useCheckout();
  const showtimeQuery = useShowtime(draft.showtimeId);
  const movie = useMovie(draft.movieId ?? showtimeQuery.data?.movieId);
  const seats = useSeats(draft.showtimeId);
  const cinemas = useCinemas();
  if (!draft.showtimeId || !draft.seatIds.length)
    return <Navigate to="/showtimes" replace />;
  const showtime = showtimeQuery.data;
  const cinema = cinemas.data?.find((item) => item.id === showtime?.cinemaId);
  const selected =
    seats.data?.filter((seat) => draft.seatIds.includes(seat.id)) ?? [];
  const subtotal = selected.reduce(
    (sum, seat) => sum + seat.price.amountMinor,
    0,
  );
  const submit = () =>
    checkout.mutate(
      { showtimeId: draft.showtimeId!, seatIds: draft.seatIds },
      {
        onSuccess: ({ checkoutUrl }) => window.location.assign(checkoutUrl),
      },
    );
  return (
    <div className="page-width page-top checkout-page">
      <div className="page-title">
        <p className="eyebrow">Thanh toán</p>
        <h1>Kiểm tra đơn hàng</h1>
        <p>
          Bạn sẽ được chuyển đến VNPay Sandbox. Trạng thái thanh toán chỉ được
          xác nhận sau khi hệ thống nhận kết quả từ máy chủ.
        </p>
      </div>
      <div className="checkout-grid">
        <section className="checkout-main">
          <h2>Phương thức thanh toán</h2>
          <div className="payment-methods">
            <label className="selected">
              <input
                type="radio"
                name="payment"
                value="VNPAY"
                checked
                disabled
                aria-label="VNPay Sandbox"
              />
              <span className="payment-mark">V</span>
              <span>
                <strong>VNPay Sandbox</strong>
                <small>Thanh toán thử nghiệm qua cổng VNPay</small>
              </span>
              <i />
            </label>
          </div>
          {checkout.error && (
            <p className="form-error" role="alert">
              {checkout.error.message}
            </p>
          )}
        </section>
        <aside className="summary-card">
          <h2>Thông tin vé</h2>
          <div className="summary-movie">
            <img src={movie.data?.posterUrl} alt="" />
            <div>
              <strong>{movie.data?.title}</strong>
              <span>
                {showtime?.format} · {movie.data?.ageRating}
              </span>
            </div>
          </div>
          <dl>
            <div>
              <dt>Rạp</dt>
              <dd>{cinema?.name}</dd>
            </div>
            <div>
              <dt>Phòng</dt>
              <dd>{showtime?.format === "IMAX" ? "IMAX Hall" : "Screen 2"}</dd>
            </div>
            <div>
              <dt>Suất chiếu</dt>
              <dd>
                {showtime &&
                  new Intl.DateTimeFormat("vi-VN", {
                    dateStyle: "medium",
                    timeStyle: "short",
                  }).format(new Date(showtime.startsAt))}
              </dd>
            </div>
            <div>
              <dt>Ghế</dt>
              <dd>{selected.map((seat) => seat.label).join(", ")}</dd>
            </div>
          </dl>
          <div className="summary-total">
            <span>Tổng thanh toán</span>
            <strong>{money(subtotal)}</strong>
          </div>
          <button
            className="primary-button wide"
            disabled={checkout.isPending || !selected.length}
            onClick={submit}
          >
            {checkout.isPending
              ? "Đang tạo phiên thanh toán…"
              : "Thanh toán qua VNPay"}
          </button>
        </aside>
      </div>
    </div>
  );
}
