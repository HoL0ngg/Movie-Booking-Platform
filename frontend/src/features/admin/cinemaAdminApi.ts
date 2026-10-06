import { HttpApiError } from '../../shared/apiClient'

const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')

export type ShowtimeStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED'
export interface CinemaSummary { id: string; name: string }
export interface CinemaDetail extends CinemaSummary { auditoriums: { id: string; name: string; seatCount: number }[] }
export interface ShowtimeDto {
  id: string; movieId: string; cinemaId: string; auditoriumId: string
  startsAt: string; endsAt: string; salesCloseAt: string
  price: { amountMinor: number; currency: string }; status: ShowtimeStatus
}
export interface CreateShowtimeInput { auditoriumId: string; movieId: string; startsAt: string; endsAt: string; salesCloseAt: string; priceMinor: number; currency: string }

async function call<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    ...init, credentials: 'omit',
    headers: { Accept: 'application/json', ...(init.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  })
  if (!response.ok) {
    const e = await response.json().catch(() => ({})) as { code?: string; message?: string; traceId?: string }
    throw new HttpApiError(e.code ?? 'HTTP_ERROR', e.message ?? 'Có lỗi xảy ra.', e.traceId ?? response.headers.get('X-Trace-Id') ?? '', response.status)
  }
  return response.json() as Promise<T>
}

export const cinemaAdminApi = {
  cinemas: () => call<CinemaSummary[]>('/cinemas'),
  cinema: (id: string) => call<CinemaDetail>(`/cinemas/${id}`),
  published: (cinemaId: string, date: string) => call<ShowtimeDto[]>(`/cinemas/${cinemaId}/showtimes?date=${date}`),
  create: (token: string, input: CreateShowtimeInput) => call<ShowtimeDto>('/showtimes', { method: 'POST', body: JSON.stringify(input) }, token),
  publish: (token: string, id: string) => call<ShowtimeDto>(`/showtimes/${id}/publish`, { method: 'POST' }, token),
  cancel: (token: string, id: string, reasonCode: string) => call<ShowtimeDto>(`/showtimes/${id}/cancel`, { method: 'POST', body: JSON.stringify({ reasonCode }) }, token),
}

export function cinemaErrorMessage(error: unknown): string {
  if (error instanceof HttpApiError) {
    if (error.status === 401) return 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.'
    if (error.status === 403) return 'Bạn không có quyền quản lý rạp này.'
    if (error.status === 404) return 'Không tìm thấy rạp, phòng chiếu hoặc suất chiếu.'
    if (error.status === 409) return error.message === 'Showtime overlaps another published showtime.' ? 'Phòng chiếu đã có suất PUBLISHED trùng giờ.' : error.message
    if (error.status === 400) return 'Dữ liệu không hợp lệ (bắt đầu < kết thúc, đóng bán vé ≤ bắt đầu).'
  }
  return 'Không thể kết nối cinema-service. Vui lòng thử lại.'
}