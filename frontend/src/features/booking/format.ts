export const money = (value: number, currency = "VND") =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency }).format(value);

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
