import { MockApiError, apiClient } from '../apiClient'
import { cinemas, movies, seatsFor, seededBooking, showtimes } from './data'
import type { Booking, Payment, Reservation, Seat, User } from '../contracts'

const STORAGE = { bookings: 'cinemat.bookings', user: 'cinemat.user', users: 'cinemat.users' }
const seatCache = new Map<string, Seat[]>()
const reservations = new Map<string, Reservation>()
const reservationKeys = new Map<string, Reservation>()
const payments = new Map<string, Payment>()
const checkoutKeys = new Map<string, { booking: Booking; payment: Payment }>()

const read = <T>(key: string, fallback: T): T => {
  const value = localStorage.getItem(key)
  return value ? JSON.parse(value) as T : fallback
}
const write = (key: string, value: unknown) => localStorage.setItem(key, JSON.stringify(value))
const getBookings = () => read<Booking[]>(STORAGE.bookings, [seededBooking])

export const mockApi = {
  movies: {
    list: (status?: 'NOW_SHOWING' | 'COMING_SOON') => apiClient.request(() => status ? movies.filter(movie => movie.status === status) : movies),
    get: (id: string) => apiClient.request(() => movies.find(movie => movie.id === id) ?? Promise.reject(new MockApiError('MOVIE_NOT_FOUND', 'Không tìm thấy phim.'))),
  },
  cinemas: {
    list: () => apiClient.request(() => cinemas),
    showtimes: (movieId: string) => apiClient.request(() => showtimes.filter(item => item.movieId === movieId)),
    showtime: (id: string) => apiClient.request(() => showtimes.find(item => item.id === id) ?? Promise.reject(new MockApiError('SHOWTIME_NOT_FOUND', 'Không tìm thấy suất chiếu.'))),
  },
  booking: {
    seats: (showtimeId: string) => apiClient.request(() => {
      if (!seatCache.has(showtimeId)) seatCache.set(showtimeId, seatsFor(showtimeId))
      return seatCache.get(showtimeId)!
    }),
    reserve: (showtimeId: string, seatIds: string[], idempotencyKey: string) => apiClient.request(() => {
      const previous = reservationKeys.get(idempotencyKey)
      if (previous) return previous
      const seats = seatCache.get(showtimeId) ?? seatsFor(showtimeId)
      const requested = seats.filter(seat => seatIds.includes(seat.id))
      if (requested.length !== seatIds.length || requested.some(seat => seat.status !== 'AVAILABLE')) throw new MockApiError('SEAT_UNAVAILABLE', 'Một hoặc nhiều ghế vừa được người khác giữ. Vui lòng chọn lại.')
      const reservation: Reservation = { id: crypto.randomUUID(), showtimeId, seatIds, status: 'HELD', holdExpiresAt: new Date(Date.now() + 10 * 60_000).toISOString() }
      requested.forEach(seat => { seat.status = 'HELD' })
      seatCache.set(showtimeId, [...seats])
      reservations.set(reservation.id, reservation)
      reservationKeys.set(idempotencyKey, reservation)
      return reservation
    }),
    checkout: (reservationId: string, method: Payment['method'], idempotencyKey: string) => apiClient.request(() => {
      const previous = checkoutKeys.get(idempotencyKey)
      if (previous) return previous
      const reservation = reservations.get(reservationId)
      if (!reservation) throw new MockApiError('RESERVATION_NOT_FOUND', 'Không tìm thấy lượt giữ ghế.')
      const showtime = showtimes.find(item => item.id === reservation.showtimeId)!
      const movie = movies.find(item => item.id === showtime.movieId)!
      const cinema = cinemas.find(item => item.id === showtime.cinemaId)!
      const seats = (seatCache.get(showtime.id) ?? []).filter(seat => reservation.seatIds.includes(seat.id))
      const bookingId = crypto.randomUUID()
      const items = seats.map(seat => ({ id: crypto.randomUUID(), seatId: seat.id, seatLabel: seat.label, price: seat.price }))
      const booking: Booking = { id: bookingId, reservationId, userId: mockApi.auth.current()?.id ?? 'guest-demo', movie: { id: movie.id, title: movie.title, posterUrl: movie.posterUrl }, cinema: { id: cinema.id, name: cinema.name, address: cinema.address }, showtime, items, total: { amountMinor: items.reduce((sum, item) => sum + item.price.amountMinor, 0), currency: 'VND' }, status: 'PAYMENT_PENDING', bookingCode: `CINE${Math.random().toString(36).slice(2, 8).toUpperCase()}`, createdAt: new Date().toISOString() }
      write(STORAGE.bookings, [booking, ...getBookings()])
      const payment: Payment = { id: crypto.randomUUID(), bookingId, method, amount: booking.total, status: 'REQUESTED' }
      payments.set(payment.id, payment)
      const result = { booking, payment }
      checkoutKeys.set(idempotencyKey, result)
      return result
    }),
    confirmMockPayment: (paymentId: string) => apiClient.request(() => {
      const payment = payments.get(paymentId)
      if (!payment) throw new MockApiError('PAYMENT_NOT_READY', 'Thanh toán chưa sẵn sàng.')
      payment.status = 'SUCCEEDED'
      const bookings = getBookings()
      const booking = bookings.find(item => item.id === payment.bookingId)!
      booking.status = 'CONFIRMED'
      const seats = seatCache.get(booking.showtime.id) ?? []
      seats.filter(seat => booking.items.some(item => item.seatId === seat.id)).forEach(seat => { seat.status = 'SOLD' })
      write(STORAGE.bookings, bookings)
      return booking
    }),
    history: () => apiClient.request(() => getBookings()),
    get: (id: string) => apiClient.request(() => getBookings().find(item => item.id === id) ?? Promise.reject(new MockApiError('BOOKING_NOT_FOUND', 'Không tìm thấy booking.'))),
  },
  auth: {
    current: () => read<User | null>(STORAGE.user, null),
    login: (email: string, password: string) => apiClient.request(() => {
      if (!email || password.length < 6) throw new MockApiError('INVALID_CREDENTIALS', 'Email hoặc mật khẩu không hợp lệ.')
      const saved = read<User[]>(STORAGE.users, [])
      const user = saved.find(item => item.email === email) ?? { id: 'user-demo', name: 'Minh Anh', email }
      const session = { id: user.id, name: user.name, email: user.email }
      write(STORAGE.user, session)
      return session
    }),
    register: (name: string, email: string, password: string) => apiClient.request(() => {
      if (!name.trim() || !email.includes('@') || password.length < 6) throw new MockApiError('VALIDATION_ERROR', 'Vui lòng nhập đủ thông tin hợp lệ.')
      const users = read<User[]>(STORAGE.users, [])
      if (users.some(item => item.email === email)) throw new MockApiError('EMAIL_EXISTS', 'Email này đã được sử dụng.')
      const user = { id: crypto.randomUUID(), name: name.trim(), email }
      write(STORAGE.users, [...users, user])
      write(STORAGE.user, { id: user.id, name: user.name, email: user.email })
      return { id: user.id, name: user.name, email: user.email }
    }),
    logout: () => apiClient.request(() => localStorage.removeItem(STORAGE.user)),
  },
}
