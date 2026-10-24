import { Link, Navigate, useSearchParams } from "react-router-dom";
import { ErrorState, LoadingState } from "../../shared/LoadingState";
import { errorMessage } from "./format";
import { statusOf, useBookingByPaymentId, usePayment } from "./hooks";

export function PaymentResultPage() {
  const [searchParams] = useSearchParams();
  const paymentId = searchParams.get("paymentId") ?? "";
  const payment = usePayment(paymentId);
  const booking = useBookingByPaymentId(
    paymentId,
    payment.data?.status === "SUCCEEDED",
  );

  if (!paymentId) {
    return (
      <div className="page-width page-top">
        <ErrorState message="Không tìm thấy mã thanh toán trong đường dẫn trả về." />
        <Link className="secondary-button" to="/bookings">
          Xem vé của tôi
        </Link>
      </div>
    );
  }

  if (payment.isLoading) {
    return (
      <div className="page-width page-top">
        <LoadingState label="Đang xác minh trạng thái thanh toán…" />
      </div>
    );
  }

  if (payment.isError || !payment.data) {
    return (
      <div className="page-width page-top">
        <ErrorState
          message={errorMessage(
            payment.error,
            "Không thể xác minh trạng thái thanh toán.",
          )}
        />
        <Link className="secondary-button" to="/bookings">
          Xem vé của tôi
        </Link>
      </div>
    );
  }

  if (payment.data.status === "SUCCEEDED") {
    if (booking.data?.status === "CONFIRMED") {
      return <Navigate to={`/booking-success/${booking.data.id}`} replace />;
    }

    if (booking.isError) {
      return (
        <div className="page-width page-top">
          <p className="eyebrow">Đã xác minh thanh toán</p>
          <h1>Đang hoàn tất booking</h1>
          <p>
            Thanh toán đã được xác minh. Booking đang được cập nhật, vui lòng
            kiểm tra lại sau ít phút.
          </p>
          {booking.error && statusOf(booking.error) !== 404 && (
            <ErrorState
              message={errorMessage(
                booking.error,
                "Chưa thể tải booking ngay lúc này.",
              )}
            />
          )}
          <button
            className="secondary-button"
            disabled={booking.isFetching}
            onClick={() => void booking.refetch()}
          >
            {booking.isFetching ? "Đang kiểm tra…" : "Kiểm tra lại"}
          </button>
        </div>
      );
    }

    return (
      <div className="page-width page-top">
        <LoadingState label="Thanh toán đã xác minh. Đang chờ booking-service xác nhận vé…" />
      </div>
    );
  }

  if (
    payment.data.status === "REQUESTED" ||
    payment.data.status === "PROVIDER_PENDING"
  ) {
    return (
      <div className="page-width page-top">
        <LoadingState label="Thanh toán đang được xử lý. Trang sẽ tự cập nhật khi có kết quả…" />
      </div>
    );
  }

  return (
    <div className="page-width page-top">
      <ErrorState
        message={`Thanh toán chưa hoàn tất (trạng thái: ${payment.data.status}).`}
      />
      <Link className="secondary-button" to="/bookings">
        Xem vé của tôi
      </Link>
    </div>
  );
}
