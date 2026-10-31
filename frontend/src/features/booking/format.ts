export const money = (value: number, currency = "VND") =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency }).format(value);

export const cinemaTimeZone = 'Asia/Ho_Chi_Minh';
export const showtimeDayKey = (date: Date) =>
  new Intl.DateTimeFormat('en-CA', { timeZone: cinemaTimeZone }).format(date);

// Today plus the next seven days, shared by the date picker and API queries.
export const showtimeDays = () => {
  const today = new Date(`${showtimeDayKey(new Date())}T00:00:00+07:00`);
  return Array.from({ length: 8 }, (_, offset) =>
    new Date(today.getTime() + offset * 86_400_000));
};

export const dateTime = (iso: string) =>
  new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(iso));

/** Lấy thông điệp lỗi hiển thị cho người dùng từ giá trị `error` bất kỳ (HttpApiError, Error, unknown). */
export const errorMessage = (
  error: unknown,
  fallback = "Đã có lỗi xảy ra. Vui lòng thử lại.",
) => (error as { message?: string } | null | undefined)?.message || fallback;
