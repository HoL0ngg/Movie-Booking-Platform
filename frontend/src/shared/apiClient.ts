import type { ApiError } from './contracts'
import { tokenStorage } from '../features/auth/tokenStorage' 

export class HttpApiError extends Error implements ApiError {
  constructor(public code: string, message: string, public traceId: string, public status: number) {
    super(message)
  }
}

const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')

export async function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, { signal, headers: { Accept: 'application/json' } })
  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as Partial<ApiError>
    throw new HttpApiError(error.code ?? 'HTTP_ERROR', error.message ?? 'Không thể tải dữ liệu.',
      error.traceId ?? response.headers.get('X-Trace-Id') ?? '', response.status)
  }
  return response.json() as Promise<T>
}

export class MockApiError extends Error implements ApiError {
  constructor(public code: string, message: string, public traceId = crypto.randomUUID()) {
    super(message)
  }
}

const delay = (ms = 180) => new Promise(resolve => window.setTimeout(resolve, ms))

export const apiClient = {
  async request<T>(handler: () => T | Promise<T>): Promise<T> {
    await delay()
    return handler()
  },
}

let refreshing: Promise<boolean> | null = null
async function tryRefresh(): Promise<boolean> {
  const t = tokenStorage.get()
  if (!t) return false
  refreshing ??= fetch(`${baseUrl}/auth/refresh`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: t.refreshToken }),
  }).then(async r => {
    if (!r.ok) { tokenStorage.clear(); return false }
    const d = await r.json()
    tokenStorage.set({ accessToken: d.accessToken, refreshToken: d.refreshToken })
    return true
  }).finally(() => { refreshing = null })
  return refreshing
}

export async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const t = tokenStorage.get()
  const res = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...(t ? { Authorization: `Bearer ${t.accessToken}` } : {}),
      ...init.headers,
    },
  })
  if (res.status === 401 && retry && t && !path.startsWith('/auth/') && await tryRefresh()) return request<T>(path, init, false)
  if (!res.ok) {
    const e = await res.json().catch(() => ({})) as Partial<ApiError>
    throw new HttpApiError(e.code ?? 'HTTP_ERROR', e.message ?? 'Có lỗi xảy ra.', e.traceId ?? '', res.status)
  }
  return res.status === 204 ? (undefined as T) : res.json()
}