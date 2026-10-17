import { HttpApiError } from '../../shared/apiClient'

export type MovieLifecycle = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export interface MovieInput {
  title: string
  synopsis: string | null
  posterUrl: string | null
  durationMinutes: number
  releaseDate: string | null
  status: MovieLifecycle
  genreIds: string[]
}
export interface AdminMovie extends MovieInput { id: string }
export interface Genre { id: string; name: string }
const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')

async function call<T>(token: string, path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${baseUrl}/admin${path}`, {
    ...init, credentials: 'omit', signal: AbortSignal.timeout(10_000),
    headers: { Accept: 'application/json', Authorization: `Bearer ${token}`, ...(init.body ? { 'Content-Type': 'application/json' } : {}) },
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as { code?: string; message?: string; traceId?: string }
    throw new HttpApiError(error.code ?? 'HTTP_ERROR', error.message ?? 'Không thể xử lý yêu cầu.', error.traceId ?? response.headers.get('X-Trace-Id') ?? '', response.status)
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}

export const movieAdminApi = {
  list: (token: string) => call<AdminMovie[]>(token, '/movies'),
  get: (token: string, id: string) => call<AdminMovie>(token, `/movies/${encodeURIComponent(id)}`),
  genres: (token: string) => call<Genre[]>(token, '/genres'),
  create: (token: string, input: MovieInput) => call<AdminMovie>(token, '/movies', { method: 'POST', body: JSON.stringify(input) }),
  update: (token: string, id: string, input: MovieInput) => call<AdminMovie>(token, `/movies/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(input) }),
  archive: (token: string, id: string) => call<void>(token, `/movies/${encodeURIComponent(id)}`, { method: 'DELETE' }),
}

export function movieAdminError(error: unknown): string {
  if (error instanceof HttpApiError) {
    const message = error.status === 401 ? 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.'
      : error.status === 403 ? 'Bạn không có quyền quản lý phim.'
      : error.status === 404 ? 'Phim không còn tồn tại. Hãy tải lại danh sách.'
      : error.status === 400 ? 'Thông tin phim không hợp lệ. Kiểm tra các trường và thể loại đã chọn.'
      : 'Không thể xử lý yêu cầu. Vui lòng thử lại.'
    return `${message}${error.traceId ? ` (Mã tra cứu: ${error.traceId})` : ''}`
  }
  return 'Không thể kết nối dịch vụ phim. Vui lòng tải lại danh sách trước khi thử lưu lại.'
}
