import { HttpApiError } from '../../shared/apiClient'
import type { Auditorium } from '../../shared/contracts'
import type { PhysicalSeat } from './seatLayout'

const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')

export type ShowtimeStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED'
export interface CinemaInput { name: string; address: string; city: string; timezone: string }
export interface AdminCinema extends CinemaInput { id: string }
export interface ShowtimeDto {
  id: string; movieId: string; cinemaId: string; auditoriumId: string
  startsAt: string; endsAt: string; salesCloseAt: string
  price: { amountMinor: number; currency: string }; status: ShowtimeStatus
}

export function cinemaErrorMessage(error: unknown): string {
  if (error instanceof HttpApiError) {
    const known: Record<string, string> = {
      LAYOUT_LOCKED: 'Phòng đã có lịch chiếu. Sơ đồ ghế chỉ được xem; tạo phòng mới nếu cần sơ đồ khác.',
      SHOWTIME_OVERLAP: 'Phòng chiếu đã có suất được phát hành trùng giờ.',
      ROOM_NAME_EXISTS: 'Tên phòng đã tồn tại trong rạp này.',
      INVALID_STATE: 'Trạng thái đã thay đổi. Tải lại danh sách trước khi thử lại.',
    }
    const message = error.status === 401 ? 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.'
      : error.status === 403 ? 'Bạn không có quyền quản lý dữ liệu này.'
      : error.status === 404 ? 'Không tìm thấy rạp, phòng, phim hoặc suất chiếu. Hãy tải lại danh sách.'
      : known[error.code] ?? (error.status === 400 ? 'Dữ liệu không hợp lệ. Kiểm tra các trường đã nhập.' : error.message)
    return `${message}${error.traceId ? ` (Mã tra cứu: ${error.traceId})` : ''}`
  }
  return 'Không thể kết nối dịch vụ. Hãy tải lại danh sách trước khi thử lưu lại.'
}
export interface CreateShowtimeInput { auditoriumId: string; movieId: string; startsAt: string; endsAt: string; salesCloseAt: string; priceMinor: number; currency: string }

async function call<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    ...init, credentials: 'omit', signal: init.signal ? AbortSignal.any([init.signal, AbortSignal.timeout(10_000)]) : AbortSignal.timeout(10_000),
    headers: { Accept: 'application/json', ...(init.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  })
  if (!response.ok) {
    const e = await response.json().catch(() => ({})) as { code?: string; message?: string; traceId?: string }
    throw new HttpApiError(e.code ?? 'HTTP_ERROR', e.message ?? 'Có lỗi xảy ra.', e.traceId ?? response.headers.get('X-Trace-Id') ?? '', response.status)
  }
  return response.json() as Promise<T>
}

export const cinemaAdminApi = {
  list: (token: string, signal?: AbortSignal) => call<AdminCinema[]>('/admin/cinemas', { signal }, token),
  saveCinema: (token: string, input: CinemaInput, id?: string) => call<AdminCinema>(`/admin/cinemas${id ? `/${encodeURIComponent(id)}` : ''}`, { method: id ? 'PUT' : 'POST', body: JSON.stringify(input) }, token),
  rooms: (token: string, cinemaId: string, signal?: AbortSignal) => call<Auditorium[]>(`/admin/cinemas/${encodeURIComponent(cinemaId)}/auditoriums`, { signal }, token),
  saveRoom: (token: string, cinemaId: string, name: string, id?: string) => call<Auditorium>(id ? `/admin/auditoriums/${encodeURIComponent(id)}` : `/admin/cinemas/${encodeURIComponent(cinemaId)}/auditoriums`, { method: id ? 'PUT' : 'POST', body: JSON.stringify({ name }) }, token),
  seats: async (token: string, roomId: string, signal?: AbortSignal) => (await call<PhysicalSeat[]>(`/admin/auditoriums/${encodeURIComponent(roomId)}/seats`, { signal }, token)).map(seat => ({ ...seat, pairedSeatId: seat.pairedSeatId ?? undefined })),
  saveSeats: (token: string, input: { auditoriumId: string; seats: PhysicalSeat[] }) => call<PhysicalSeat[]>(`/admin/auditoriums/${encodeURIComponent(input.auditoriumId)}/seats`, { method: 'PUT', body: JSON.stringify({ seats: input.seats }) }, token),
  showtimes: (token: string, cinemaId: string, signal?: AbortSignal) => call<ShowtimeDto[]>(`/admin/showtimes${cinemaId ? `?cinemaId=${encodeURIComponent(cinemaId)}` : ''}`, { signal }, token),
  update: (token: string, id: string, input: CreateShowtimeInput) => call<ShowtimeDto>(`/admin/showtimes/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(input) }, token),
  create: (token: string, input: CreateShowtimeInput) => call<ShowtimeDto>('/showtimes', { method: 'POST', body: JSON.stringify(input) }, token),
  publish: (token: string, id: string) => call<ShowtimeDto>(`/showtimes/${id}/publish`, { method: 'POST' }, token),
  cancel: (token: string, id: string, reasonCode: string) => call<ShowtimeDto>(`/showtimes/${id}/cancel`, { method: 'POST', body: JSON.stringify({ reasonCode }) }, token),
}
