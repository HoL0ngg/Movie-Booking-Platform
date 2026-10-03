import type { ApiError } from '../types/contracts'

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
