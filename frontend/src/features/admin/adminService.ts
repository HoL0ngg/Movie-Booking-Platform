import { apiClient, MockApiError } from '../../shared/apiClient'
import { auditoriums, cinemas, movies, seededBooking, seatsFor, showtimes } from '../../shared/mocks/data'
import type { Auditorium, Booking, Cinema, Movie, Payment, Showtime } from '../../shared/contracts'
import type { PhysicalSeat } from './seatLayout'

export type AdminPayment = Payment & { createdAt: string; reference: string }
export interface AdminData {
  movies: Movie[]
  cinemas: Cinema[]
  auditoriums: Auditorium[]
  seats: PhysicalSeat[]
  showtimes: Showtime[]
  bookings: Booking[]
  payments: AdminPayment[]
}
const storageKey = 'cinemat.admin-demo.v1'
const demoSeats: PhysicalSeat[] = auditoriums.flatMap(room => seatsFor('').map(seat => ({
  id: `${room.id}-${seat.label}`, auditoriumId: room.id, rowLabel: seat.row, seatNumber: seat.number,
  seatType: seat.type, isActive: true,
})))

// Isolated demo fixtures, never a source of booking or payment truth.
function seed(): AdminData {
  const bookings: Booking[] = Array.from({ length: 18 }, (_, index) => {
    const showtime = showtimes[index % showtimes.length]
    const movie = movies.find(item => item.id === showtime.movieId)!
    const cinema = cinemas.find(item => item.id === showtime.cinemaId)!
    const status = index % 6 === 0 ? 'CANCELLED' : index % 5 === 0 ? 'PAYMENT_PENDING' : 'CONFIRMED'
    const createdAt = new Date()
    createdAt.setDate(createdAt.getDate() - Math.floor(index / 3))
    createdAt.setHours(9 + index % 10, 15, 0, 0)
    const items = ['D5', 'D6'].map((label, itemIndex) => ({ id: `demo-item-${index}-${itemIndex}`, seatId: `demo-seat-${index}-${label}`, seatLabel: label, price: showtime.price }))
    return { ...seededBooking, id: `demo-booking-${index + 1}`, reservationId: `demo-reservation-${index + 1}`, userId: `demo-customer-${index + 1}`, bookingCode: `CM${String(10240 + index)}`, movie, cinema, showtime, items, total: { amountMinor: showtime.price.amountMinor * items.length, currency: 'VND' }, status, createdAt: createdAt.toISOString() }
  })
  const payments: AdminPayment[] = bookings.map((booking, index) => ({ id: `demo-payment-${index + 1}`, bookingId: booking.id, method: index % 2 ? 'MOMO' : 'VNPAY', amount: booking.total, status: booking.status === 'CONFIRMED' ? 'SUCCEEDED' : booking.status === 'CANCELLED' ? 'FAILED' : 'REQUESTED', createdAt: booking.createdAt, reference: `DEMO-${String(800001 + index)}` }))
  return structuredClone({ movies, cinemas, auditoriums, seats: demoSeats, showtimes, bookings, payments })
}

function read(): AdminData {
  try {
    const value = localStorage.getItem(storageKey)
    if (value) {
      const data = JSON.parse(value) as AdminData
      if (!data || !['movies', 'cinemas', 'auditoriums', 'showtimes', 'bookings', 'payments'].every(key => Array.isArray(data[key as keyof AdminData]))) throw new Error('Invalid demo data')
      // Restore missing layouts only for known demo rooms; preserve saved and custom layouts.
      data.seats ??= []
      if (!Array.isArray(data.seats)) throw new Error('Invalid seat data')
      data.seats.push(...demoSeats.filter(seat => data.auditoriums.some(room => room.id === seat.auditoriumId)
        && !data.seats.some(item => item.auditoriumId === seat.auditoriumId)))
      return data
    }
    const data = seed()
    localStorage.setItem(storageKey, JSON.stringify(data))
    return data
  } catch {
    throw new MockApiError('DEMO_STORAGE_UNAVAILABLE', 'Không thể đọc dữ liệu demo. Hãy cho phép trình duyệt lưu dữ liệu hoặc xóa dữ liệu demo bị lỗi.')
  }
}

export const adminService = { list: () => apiClient.request(read) }
