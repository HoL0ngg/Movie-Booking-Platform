import type { ApiError } from '../types/contracts'

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
