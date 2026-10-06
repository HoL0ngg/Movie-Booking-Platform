import { mockApi } from '../../shared/mocks/api'
import type { Payment } from '../../shared/contracts'

export { cinemaService } from './cinemaApi'
export const seatService = { getByShowtime: mockApi.booking.seats }
export const bookingService = {
  reserve: mockApi.booking.reserve,
  checkout: mockApi.booking.checkout,
  history: mockApi.booking.history,
  get: mockApi.booking.get,
}
export const paymentService = { confirmMock: mockApi.booking.confirmMockPayment }
export type PaymentMethod = Payment['method']
