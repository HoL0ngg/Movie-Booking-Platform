import { apiClient, MockApiError } from '../../api/client'
import { auditoriums, cinemas, movies, seededBooking, showtimes } from '../../mocks/data'
import type { Auditorium, Booking, Cinema, Movie, Payment, Showtime } from '../../types/contracts'

export type AdminPayment = Payment & { createdAt: string; reference: string }
export interface AdminData {
  movies: Movie[]
  cinemas: Cinema[]
  auditoriums: Auditorium[]
  showtimes: Showtime[]
  bookings: Booking[]
  payments: AdminPayment[]
}
export type CatalogKind = 'movies' | 'cinemas' | 'showtimes'
export type CatalogRecord = Movie | Cinema | Showtime
const storageKey = 'cinemat.admin-demo.v1'

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
  return structuredClone({ movies, cinemas, auditoriums, showtimes, bookings, payments })
}

function read(): AdminData {
  try {
    const value = localStorage.getItem(storageKey)
    if (value) {
      const data = JSON.parse(value) as AdminData
      if (!data || !['movies', 'cinemas', 'auditoriums', 'showtimes', 'bookings', 'payments'].every(key => Array.isArray(data[key as keyof AdminData]))) throw new Error('Invalid demo data')
      return data
    }
    const data = seed()
    localStorage.setItem(storageKey, JSON.stringify(data))
    return data
  } catch {
    throw new MockApiError('DEMO_STORAGE_UNAVAILABLE', 'Không thể đọc dữ liệu demo. Hãy cho phép trình duyệt lưu dữ liệu hoặc xóa dữ liệu demo bị lỗi.')
  }
}

export const adminService = {
  list: () => apiClient.request(read),
  save: (input: { kind: CatalogKind; record: CatalogRecord }) => apiClient.request(() => {
    const data = read()
    const { kind, record } = input
    if (kind === 'showtimes') {
      const slot = record as Showtime
      if (!data.movies.some(item => item.id === slot.movieId) || !data.auditoriums.some(item => item.id === slot.auditoriumId && item.cinemaId === slot.cinemaId)) {
        throw new MockApiError('INVALID_REFERENCE', 'Vui lòng chọn phim, rạp và phòng chiếu hợp lệ.')
      }
      if (!Number.isFinite(Date.parse(slot.startsAt)) || !Number.isFinite(Date.parse(slot.endsAt)) || Date.parse(slot.endsAt) <= Date.parse(slot.startsAt) || !Number.isSafeInteger(slot.price.amountMinor) || slot.price.amountMinor <= 0) {
        throw new MockApiError('INVALID_SHOWTIME', 'Thời gian hoặc giá vé không hợp lệ.')
      }
      if (data.showtimes.some(item => item.id !== slot.id && item.auditoriumId === slot.auditoriumId && Date.parse(item.startsAt) < Date.parse(slot.endsAt) && Date.parse(item.endsAt) > Date.parse(slot.startsAt))) {
        throw new MockApiError('SHOWTIME_OVERLAP', 'Phòng chiếu đã có suất chiếu trong khoảng thời gian này.')
      }
      if (data.bookings.some(item => item.showtime.id === slot.id)) {
        throw new MockApiError('SHOWTIME_HAS_BOOKINGS', 'Suất chiếu đã có đơn đặt vé demo. Vui lòng tạo suất chiếu mới.')
      }
      data.showtimes = [...data.showtimes.filter(item => item.id !== slot.id), slot]
    } else if (kind === 'movies') {
      const movie = record as Movie
      if (!movie.title.trim() || !Number.isSafeInteger(movie.durationMinutes) || movie.durationMinutes <= 0) throw new MockApiError('INVALID_MOVIE', 'Tên phim và thời lượng phải hợp lệ.')
      const previous = data.movies.find(item => item.id === movie.id)
      if (previous && previous.durationMinutes !== movie.durationMinutes && data.showtimes.some(item => item.movieId === movie.id)) throw new MockApiError('MOVIE_HAS_SHOWTIMES', 'Phim đã có lịch chiếu. Giữ nguyên thời lượng để tránh thay đổi lịch đã tạo.')
      data.movies = [movie, ...data.movies.filter(item => item.id !== movie.id)]
    } else {
      const cinema = record as Cinema
      if (!cinema.name.trim() || !cinema.address.trim() || !cinema.city.trim()) throw new MockApiError('INVALID_CINEMA', 'Vui lòng nhập đầy đủ thông tin rạp.')
      if (!data.cinemas.some(item => item.id === cinema.id)) data.auditoriums.push({ id: crypto.randomUUID(), cinemaId: cinema.id, name: 'Phòng 01' })
      data.cinemas = [cinema, ...data.cinemas.filter(item => item.id !== cinema.id)]
    }
    try { localStorage.setItem(storageKey, JSON.stringify(data)) }
    catch { throw new MockApiError('DEMO_SAVE_FAILED', 'Không thể lưu thay đổi demo. Kiểm tra dung lượng và quyền lưu dữ liệu của trình duyệt.') }
    return data
  }),
}
