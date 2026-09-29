import { mockApi } from '../mocks/api'
import type { Payment } from '../types/contracts'

// Keep components and hooks independent from the transport. Replace mockApi here with an HTTP client later.
export const movieService = mockApi.movies
export const cinemaService = mockApi.cinemas
export const seatService = { getByShowtime: mockApi.booking.seats }
export const bookingService = {
  reserve: mockApi.booking.reserve,
  checkout: mockApi.booking.checkout,
  history: mockApi.booking.history,
  get: mockApi.booking.get,
}
export const paymentService = { confirmMock: mockApi.booking.confirmMockPayment }
export const authService = mockApi.auth
export type PaymentMethod = Payment['method']
