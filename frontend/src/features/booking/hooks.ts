import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef } from "react";
import {
  bookingService,
  cinemaService,
  paymentService,
  seatService,
  type CreatePaymentRequest,
} from "./api";
import { useMovie } from "../movies/hooks";

// Không phụ thuộc tên thuộc tính cụ thể của HttpApiError: chỉ cần có trường `status`.
export const statusOf = (error: unknown): number | undefined =>
  (error as { status?: number } | null)?.status;

// Lỗi 4xx (404, 409, 410, 422…) là lỗi nghiệp vụ, thử lại vô ích → không retry.
const retryUnlessClientError = (failureCount: number, error: unknown) => {
  const status = statusOf(error);
  return (
    !(status !== undefined && status >= 400 && status < 500) && failureCount < 2
  );
};

export const useCinemas = () =>
  useQuery({ queryKey: ["cinemas"], queryFn: cinemaService.list });

export const useShowtimes = (movieId = "") =>
  useQuery({
    queryKey: ["showtimes", movieId],
    queryFn: () => cinemaService.showtimes(movieId),
    enabled: Boolean(movieId),
  });

export const useShowtime = (id = "") =>
  useQuery({
    queryKey: ["showtime", id],
    queryFn: () => cinemaService.showtime(id),
    enabled: Boolean(id),
    retry: retryUnlessClientError,
  });

// Ghế thay đổi liên tục (người khác giữ/đặt) → làm mới định kỳ khi tab đang mở.
export const useSeats = (showtimeId = "") =>
  useQuery({
    queryKey: ["seats", showtimeId],
    queryFn: ({ signal }) => seatService.getByShowtime(showtimeId, signal),
    // Chỉ làm mới khi có showtimeId, tránh gọi API vô nghĩa.
    enabled: Boolean(showtimeId),
    // Làm mới mỗi 15 giây. Giúp cập nhật trạng thái ghế mới nhất khi người dùng đang xem sơ đồ ghế.
    // 15000ms
    refetchInterval: 15_000,
    // Lỗi 4xx (404, 409, 410, 422…) là lỗi nghiệp vụ, thử lại vô ích → không retry.
    retry: retryUnlessClientError,
  });

export const useBooking = (id = "") =>
  useQuery({
    queryKey: ["booking", id],
    queryFn: ({ signal }) => bookingService.get(id, signal),
    enabled: Boolean(id),
    retry: retryUnlessClientError,
  });

export const useBookingByPaymentId = (
  paymentId = "",
  enabled = true,
) =>
  useQuery({
    queryKey: ["booking-by-payment", paymentId],
    queryFn: ({ signal }) => bookingService.getByPaymentId(paymentId, signal),
    enabled: Boolean(paymentId) && enabled,
    retry: (failureCount, error) =>
      statusOf(error) === 404 && failureCount < 10,
    retryDelay: 1500,
  });

export const usePayment = (paymentId = "") =>
  useQuery({
    queryKey: ["payment", paymentId],
    queryFn: ({ signal }) => paymentService.get(paymentId, signal),
    enabled: Boolean(paymentId),
    retry: (failureCount, error) => {
      const code = (error as { code?: string } | null)?.code;
      if (code === "PAYMENT_NOT_READY") return failureCount < 5;
      return retryUnlessClientError(failureCount, error);
    },
    retryDelay: (failureCount) => Math.min(1000 * 2 ** failureCount, 5000),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "REQUESTED" || status === "PROVIDER_PENDING"
        ? 3000
        : false;
    },
  });

export const usePaymentRefunds = (paymentId = "") =>
  useQuery({
    queryKey: ["payment", paymentId, "refunds"],
    queryFn: ({ signal }) => paymentService.refunds(paymentId, signal),
    enabled: Boolean(paymentId),
    retry: retryUnlessClientError,
  });

export const useCreatePayment = () => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: ({
      payload,
      idempotencyKey,
    }: {
      payload: CreatePaymentRequest;
      idempotencyKey: string;
    }) => paymentService.create(payload, idempotencyKey),
    onSuccess: (payment) => {
      client.setQueryData(["payment", payment.paymentId], payment);
    },
  });
};

export const useBookingHistory = () =>
  useQuery({
    queryKey: ["bookings"],
    queryFn: ({ signal }) => bookingService.history(signal),
  });

/** Gom thông tin hiển thị của một suất chiếu (suất + phim + rạp) cho trang vé / lịch sử. */
export function useShowtimeContext(showtimeId = "") {
  const showtime = useShowtime(showtimeId);
  const movie = useMovie(showtime.data?.movieId);
  const cinemas = useCinemas();
  return {
    showtime: showtime.data,
    movie: movie.data,
    cinema: cinemas.data?.find((item) => item.id === showtime.data?.cinemaId),
    showtimeError: showtime.error,
    isLoading: showtime.isLoading || movie.isLoading || cinemas.isLoading,
  };
}

// Mỗi "lần thử đặt vé" có bộ khoá riêng. Cùng ghế + cùng suất → dùng lại khoá, nên bấm lại sau lỗi mạng
// là an toàn (backend trả đúng reservation cũ, không tạo thêm). Đổi ghế → khoá mới, tránh lỗi 422
// "Idempotency-Key đã được dùng với request khác".
interface Attempt {
  signature: string;
  reservationKey: string;
  paymentKey: string;
}
const newAttempt = (signature: string): Attempt => ({
  signature,
  reservationKey: crypto.randomUUID(),
  paymentKey: crypto.randomUUID(),
});

export const useCheckout = () => {
  const client = useQueryClient();
  const attempt = useRef<Attempt | null>(null);

  return useMutation({
    mutationFn: async ({
      showtimeId,
      seatIds,
    }: {
      showtimeId: string;
      seatIds: string[];
    }) => {
      const signature = `${showtimeId}:${[...seatIds].sort().join(",")}`;
      if (attempt.current?.signature !== signature)
        attempt.current = newAttempt(signature);

      const reserve = async (reservationKey: string) => {
        try {
          return await bookingService.reserve(
            showtimeId,
            seatIds,
            reservationKey,
          );
        } catch (error) {
          const status = statusOf(error);
          if (status === 409 || status === 410 || status === 422)
            attempt.current = null;
          throw error;
        }
      };

      let { reservationKey, paymentKey } = attempt.current;
      let reservation = await reserve(reservationKey);

      // Lần thử trước đã giữ ghế nhưng hết hạn trong lúc chờ → xin giữ lại bằng khoá mới.
      if (Date.parse(reservation.holdExpiresAt) <= Date.now()) {
        attempt.current = newAttempt(signature);
        ({ reservationKey, paymentKey } = attempt.current);
        reservation = await reserve(reservationKey);
      }
      const payment = await paymentService.create(
        {
          reservationId: reservation.id,
          provider: "vnpay",
          locale: "vn",
        },
        paymentKey,
      );

      if (!payment.checkout?.url)
        throw new Error("Payment service chưa trả về đường dẫn thanh toán VNPay.");

      let checkoutUrl: URL;
      try {
        checkoutUrl = new URL(payment.checkout.url);
      } catch {
        throw new Error("Đường dẫn thanh toán VNPay không hợp lệ.");
      }
      if (
        checkoutUrl.protocol !== "https:" ||
        checkoutUrl.hostname !== "sandbox.vnpayment.vn"
      ) {
        throw new Error("Chỉ chấp nhận đường dẫn VNPay Sandbox hợp lệ.");
      }

      return { payment, checkoutUrl: checkoutUrl.href };
    },
    onSuccess: ({ payment }, variables) => {
      attempt.current = null;
      client.setQueryData(["payment", payment.paymentId], payment);
      void client.invalidateQueries({
        queryKey: ["seats", variables.showtimeId],
      });
    },
    onError: (error, variables) => {
      const status = statusOf(error);
      if (status === 409 || status === 410 || status === 422) {
        void client.invalidateQueries({
          queryKey: ["seats", variables.showtimeId],
        }); // cập nhật sơ đồ ghế
      }
      // Lỗi mạng / 5xx: giữ nguyên khoá để bấm lại vẫn idempotent
    },
  });
};
