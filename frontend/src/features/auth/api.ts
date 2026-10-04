import { mockApi } from '../../shared/mocks/api'
import { HttpApiError } from '../../shared/apiClient'

export interface AdminToken { accessToken: string; tokenType: 'Bearer'; expiresIn: number }
export interface AdminProfile { id: string; email: string; roles: string[] }
const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')

async function request(path: string, options: RequestInit): Promise<unknown> {
  const response = await fetch(`${baseUrl}${path}`, {
    ...options, credentials: 'omit', signal: AbortSignal.timeout(10_000),
    headers: { Accept: 'application/json', ...options.headers },
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    throw new HttpApiError(typeof error?.code === 'string' ? error.code : 'HTTP_ERROR', 'Không thể xác thực.',
      typeof error?.traceId === 'string' ? error.traceId : response.headers.get('X-Trace-Id') ?? '', response.status)
  }
  return response.json()
}

export const adminAuthApi = {
  async login(email: string, password: string): Promise<AdminToken> {
    const data = await request('/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email.trim(), password }),
    }) as Partial<AdminToken> | null
    if (!data || typeof data.accessToken !== 'string' || !data.accessToken || data.tokenType !== 'Bearer'
      || !Number.isInteger(data.expiresIn) || data.expiresIn! <= 0 || data.expiresIn! > 86400) {
      throw new Error('INVALID_AUTH_RESPONSE')
    }
    return data as AdminToken
  },
  async profile(token: string): Promise<AdminProfile> {
    const data = await request('/me', { headers: { Authorization: `Bearer ${token}` } }) as Partial<AdminProfile> | null
    if (!data || typeof data.id !== 'string' || typeof data.email !== 'string'
      || !Array.isArray(data.roles) || !data.roles.every(role => typeof role === 'string')) {
      throw new Error('INVALID_AUTH_RESPONSE')
    }
    if (!data.roles.includes('ADMIN')) throw new HttpApiError('FORBIDDEN', 'Cần quyền quản trị viên.', '', 403)
    return data as AdminProfile
  },
}

export function adminAuthError(error: Error): string {
  if (error instanceof HttpApiError) {
    if (error.status === 401) return 'Email hoặc mật khẩu không đúng. Vui lòng thử lại.'
    if (error.status === 403) return 'Tài khoản không có quyền truy cập quản trị.'
    if (error.status === 429) return 'Bạn đã thử quá nhiều lần. Vui lòng chờ rồi đăng nhập lại.'
    if (error.status === 400) return 'Thông tin đăng nhập không hợp lệ. Vui lòng kiểm tra lại.'
  }
  return 'Không thể kết nối dịch vụ đăng nhập. Vui lòng thử lại sau.'
}

export const authService = mockApi.auth
