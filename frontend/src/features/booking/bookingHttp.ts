import { HttpApiError } from "../../shared/apiClient";
import { getCurrentUserCredentials } from "./currentUser";
// Cùng gốc /api/v1 với cinemaApi (qua gateway). Đổi bằng VITE_API_BASE_URL nếu cần.
const BASE = (
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "/api/v1"
).replace(/\/$/, "");

interface RequestOptions {
  method?: "GET" | "POST" | "DELETE";
  body?: unknown;
  idempotencyKey?: string;
  signal?: AbortSignal;
}

interface ErrorBody {
  code?: string;
  message?: string;
  detail?: string;
  title?: string;
  traceId?: string;
}

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

/** Gọi Booking Service với bearer token, X-User-Id / Idempotency-Key và chuẩn hóa lỗi. */
export async function bookingRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { method = "GET", body, idempotencyKey, signal } = options;
  const { userId, accessToken: token } = await getCurrentUserCredentials();
  const headers: Record<string, string> = {
    Accept: "application/json",
    "X-User-Id": userId,
    Authorization: `Bearer ${token}`,
  };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (idempotencyKey) headers["Idempotency-Key"] = idempotencyKey;

  let response: Response;
  try {
    response = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError")
      throw error;
    throw new HttpApiError(
      "NETWORK_ERROR",
      "Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng và thử lại.",
      "",
      0,
    );
  }

  if (response.status === 204) return undefined as T;
  const text = await response.text();
  const data = text ? parseJson(text) : undefined;

  if (!response.ok) {
    const error = (data ?? {}) as ErrorBody;
    throw new HttpApiError(
      error.code ?? `HTTP_${response.status}`,
      error.message ??
        error.detail ??
        error.title ??
        `Yêu cầu thất bại (${response.status}).`,
      error.traceId ?? response.headers.get("X-Trace-Id") ?? "",
      response.status,
    );
  }
  return data as T;
}
