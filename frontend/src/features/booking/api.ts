export { cinemaService } from "./cinemaApi";
import { request } from "../../shared/apiClient";
import { bookingRequest } from "./bookingHttp";

// ---------- Kiểu dữ liệu dùng trong UI ----------
export type PaymentMethod = "MOMO" | "VNPAY" | "CARD_AT_COUNTER";

export interface SeatInfo {
  id: string;
  row: string;
  number: number;
  label: string;
  type: string;
  status: string; // AVAILABLE | HELD | PAYMENT_PENDING | SOLD
  price: { amountMinor: number; currency: string };
}

export interface BookedSeat {
  seatId: string;
  seatLabel: string;
  priceMinor: number;
}

export interface Reservation {
  id: string;
  showtimeId: string;
  status: string;
  holdExpiresAt: string;
  totalMinor: number;
  seats: BookedSeat[];
}

export interface Booking {
  id: string;
  reservationId: string;
  showtimeId: string;
  status: string; // CONFIRMED | PAYMENT_PENDING | CANCELLED | REFUND_PENDING | REFUNDED
  amountMinor: number;
  currency: string;
  confirmedAt: string | null;
  seats: BookedSeat[];
  bookingCode: string;
}

export interface Payment {
  paymentId: string;
  reservationId: string;
  status: string;
  amountMinor: number;
  currency: string;
  checkout: { provider: string; url: string } | null;
  failureCode: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentRequest {
  reservationId: string;
  provider?: string;
  bankCode?: string;
  locale?: "vn" | "en";
}

export interface PaymentRefund {
  refundId: string;
  paymentId: string;
  status: string;
  amountMinor: number;
  currency: string;
  failureCode: string | null;
  createdAt: string;
  updatedAt: string;
}

export const BOOKING_STATUS_LABEL: Record<string, string> = {
  CONFIRMED: "Đã xác nhận",
  PAYMENT_PENDING: "Chờ thanh toán",
  CANCELLED: "Đã huỷ",
  REFUND_PENDING: "Đang hoàn tiền",
  REFUNDED: "Đã hoàn tiền",
};

// ---------- DTO của Booking Service ----------
interface SeatDto {
  showtimeId: string;
  seatId: string;
  seatLabel: string;
  seatType: string;
  priceMinor: number;
  status: string;
}
interface ReservationDto {
  reservationId: string;
  showtimeId: string;
  status: string;
  holdExpiresAt: string;
  totalMinor: number;
  seats: BookedSeat[];
}
interface BookingDto {
  bookingId: string;
  reservationId: string;
  showtimeId: string;
  status: string;
  amountMinor: number;
  currency: string;
  confirmedAt: string | null;
  seats: BookedSeat[];
}

// ---------- Mapper ----------
const parseLabel = (label: string) => {
  const match = /^([A-Za-z]+)(\d+)$/.exec(label.trim());
  return match
    ? { row: match[1].toUpperCase(), number: Number(match[2]) }
    : { row: label, number: 0 };
};

const toSeat = (dto: SeatDto): SeatInfo => ({
  id: dto.seatId,
  label: dto.seatLabel,
  type: dto.seatType,
  status: dto.status,
  price: { amountMinor: dto.priceMinor, currency: "VND" },
  ...parseLabel(dto.seatLabel),
});

// API không đảm bảo thứ tự ghế → sắp theo hàng (A, B, …, AA) rồi số ghế.
const bySeatOrder = (a: SeatInfo, b: SeatInfo) =>
  a.row.length - b.row.length ||
  a.row.localeCompare(b.row) ||
  a.number - b.number;

const bySeatLabel = (a: BookedSeat, b: BookedSeat) =>
  a.seatLabel.localeCompare(b.seatLabel, undefined, { numeric: true });

const toReservation = (dto: ReservationDto): Reservation => ({
  id: dto.reservationId,
  showtimeId: dto.showtimeId,
  status: dto.status,
  holdExpiresAt: dto.holdExpiresAt,
  totalMinor: dto.totalMinor,
  seats: [...dto.seats].sort(bySeatLabel),
});

// Backend chưa có mã vé riêng → dùng 8 ký tự đầu của bookingId làm mã hiển thị.
const toBooking = (dto: BookingDto): Booking => ({
  id: dto.bookingId,
  reservationId: dto.reservationId,
  showtimeId: dto.showtimeId,
  status: dto.status,
  amountMinor: dto.amountMinor,
  currency: dto.currency,
  confirmedAt: dto.confirmedAt,
  seats: [...dto.seats].sort(bySeatLabel),
  bookingCode: dto.bookingId.replaceAll("-", "").slice(0, 8).toUpperCase(),
});

// ---------- Service ----------
export const seatService = {
  async getByShowtime(
    showtimeId: string,
    signal?: AbortSignal,
  ): Promise<SeatInfo[]> {
    const dtos = await bookingRequest<SeatDto[]>(
      `/showtimes/${encodeURIComponent(showtimeId)}/seats`,
      { signal },
    );
    return dtos.map(toSeat).sort(bySeatOrder);
  },
};

export const bookingService = {
  /** Giữ ghế. Idempotent theo idempotencyKey. */
  async reserve(
    showtimeId: string,
    seatIds: string[],
    idempotencyKey: string,
  ): Promise<Reservation> {
    return toReservation(
      await bookingRequest<ReservationDto>("/reservations", {
        method: "POST",
        body: { showtimeId, seatIds },
        idempotencyKey,
      }),
    );
  },

  /** Xác nhận booking sau khi thanh toán. Idempotent theo reservation. */
  async confirm(reservationId: string, paymentId: string): Promise<Booking> {
    return toBooking(
      await bookingRequest<BookingDto>(
        `/reservations/${encodeURIComponent(reservationId)}/bookings`,
        {
          method: "POST",
          body: { paymentId },
        },
      ),
    );
  },

  async cancel(reservationId: string): Promise<void> {
    await bookingRequest<void>(
      `/reservations/${encodeURIComponent(reservationId)}`,
      { method: "DELETE" },
    );
  },

  async history(signal?: AbortSignal): Promise<Booking[]> {
    return (await bookingRequest<BookingDto[]>("/bookings", { signal })).map(
      toBooking,
    );
  },

  async get(id: string, signal?: AbortSignal): Promise<Booking> {
    return toBooking(
      await bookingRequest<BookingDto>(`/bookings/${encodeURIComponent(id)}`, {
        signal,
      }),
    );
  },

  async getByPaymentId(
    paymentId: string,
    signal?: AbortSignal,
  ): Promise<Booking> {
    return toBooking(
      await bookingRequest<BookingDto>(
        `/bookings/by-payment/${encodeURIComponent(paymentId)}`,
        { signal },
      ),
    );
  },
};

export const paymentService = {
  async create(
    payload: CreatePaymentRequest,
    idempotencyKey: string,
  ): Promise<Payment> {
    return request<Payment>("/payments", {
      method: "POST",
      body: JSON.stringify(payload),
      headers: { "Idempotency-Key": idempotencyKey },
    });
  },

  async get(paymentId: string, signal?: AbortSignal): Promise<Payment> {
    return request<Payment>(
      `/payments/${encodeURIComponent(paymentId)}`,
      { signal },
    );
  },

  async refunds(
    paymentId: string,
    signal?: AbortSignal,
  ): Promise<PaymentRefund[]> {
    return request<PaymentRefund[]>(
      `/payments/${encodeURIComponent(paymentId)}/refunds`,
      { signal },
    );
  },
};
