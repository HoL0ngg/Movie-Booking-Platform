import { apiClient, MockApiError } from '../../shared/apiClient'
import { auditoriums, cinemas, movies, seededBooking, showtimes } from '../../shared/mocks/data'
import type { Auditorium, Booking, Cinema, Movie, Payment, SeatType, Showtime } from '../../shared/contracts'

export type AdminPayment = Payment & { createdAt: string; reference: string }
export interface PhysicalSeat { id: string; auditoriumId: string; rowLabel: string; seatNumber: number; seatType: SeatType; isActive: boolean; pairedSeatId?: string }
export interface AdminData {
  movies: Movie[]
  cinemas: Cinema[]
  auditoriums: Auditorium[]
  seats: PhysicalSeat[]
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
  return structuredClone({ movies, cinemas, auditoriums, seats: [], showtimes, bookings, payments })
}

function read(): AdminData {
  try {
    const value = localStorage.getItem(storageKey)
    if (value) {
      const data = JSON.parse(value) as AdminData
      if (!data || !['movies', 'cinemas', 'auditoriums', 'showtimes', 'bookings', 'payments'].every(key => Array.isArray(data[key as keyof AdminData]))) throw new Error('Invalid demo data')
      // Older browser fixtures have no physical seat layouts.
      data.seats ??= []
      if (!Array.isArray(data.seats)) throw new Error('Invalid seat data')
      return data
    }
    const data = seed()
    localStorage.setItem(storageKey, JSON.stringify(data))
    return data
  } catch {
    throw new MockApiError('DEMO_STORAGE_UNAVAILABLE', 'Không thể đọc dữ liệu demo. Hãy cho phép trình duyệt lưu dữ liệu hoặc xóa dữ liệu demo bị lỗi.')
  }
}

function persist(data: AdminData): AdminData {
  try { localStorage.setItem(storageKey, JSON.stringify(data)) }
  catch { throw new MockApiError('DEMO_SAVE_FAILED', 'Không thể lưu thay đổi. Kiểm tra dung lượng và quyền lưu dữ liệu của trình duyệt.') }
  return data
}

export function changeSeatType(seats: PhysicalSeat[], selectedIds: string[], type: SeatType): PhysicalSeat[] {
  const selected = seats.filter(seat => selectedIds.includes(seat.id))
  if (!selected.length) throw new MockApiError('NO_SEATS_SELECTED', 'Chọn ghế trước khi đổi loại.')
  if (selected.some(seat => seat.pairedSeatId && !selectedIds.includes(seat.pairedSeatId))) throw new MockApiError('INCOMPLETE_PAIR', 'Hãy chọn cả hai ghế trong cặp để đổi loại.')
  if (type === 'COUPLE' && (selected.length !== 2 || selected[0].rowLabel !== selected[1].rowLabel || Math.abs(selected[0].seatNumber - selected[1].seatNumber) !== 1 || selected.some(seat => !seat.isActive))) throw new MockApiError('INVALID_COUPLE', 'Ghế đôi cần đúng 2 ghế đang sử dụng, liền nhau trong cùng hàng.')
  return seats.map(seat => selectedIds.includes(seat.id) ? { ...seat, seatType: type, pairedSeatId: type === 'COUPLE' ? selected.find(item => item.id !== seat.id)!.id : undefined } : seat)
}

export const adminService = {
  list: () => apiClient.request(read),
  saveRoom: (room: Auditorium) => apiClient.request(() => {
    const data = read()
    const name = room.name.trim()
    if (!room.id || !name || name.length > 100 || !data.cinemas.some(item => item.id === room.cinemaId)) throw new MockApiError('INVALID_ROOM', 'Tên phòng hoặc rạp không hợp lệ.')
    if (data.auditoriums.some(item => item.id === room.id && item.cinemaId !== room.cinemaId)) throw new MockApiError('INVALID_ROOM', 'Không thể chuyển phòng sang rạp khác.')
    if (data.auditoriums.some(item => item.id !== room.id && item.cinemaId === room.cinemaId && item.name.trim().toLocaleLowerCase('vi-VN') === name.toLocaleLowerCase('vi-VN'))) throw new MockApiError('ROOM_NAME_EXISTS', 'Tên phòng đã tồn tại trong rạp này.')
    data.auditoriums = [...data.auditoriums.filter(item => item.id !== room.id), { ...room, name }]
    return persist(data)
  }),
  saveSeats: (input: { auditoriumId: string; seats: PhysicalSeat[] }) => apiClient.request(() => {
    const data = read()
    if (!data.auditoriums.some(item => item.id === input.auditoriumId)) throw new MockApiError('ROOM_NOT_FOUND', 'Không tìm thấy phòng chiếu.')
    if (data.showtimes.some(item => item.auditoriumId === input.auditoriumId)) throw new MockApiError('ROOM_HAS_SHOWTIMES', 'Phòng đã có lịch chiếu. Hãy tạo phòng mới để thiết kế sơ đồ khác.')
    if (!input.seats.length || input.seats.length > 780) throw new MockApiError('INVALID_LAYOUT', 'Sơ đồ cần từ 1 đến 780 ghế.')
    const ids = new Set<string>()
    const positions = new Set<string>()
    const seats = input.seats.map(seat => ({ ...seat, rowLabel: seat.rowLabel.trim().toUpperCase() }))
    for (const seat of seats) {
      const position = `${seat.rowLabel}:${seat.seatNumber}`
      if (!seat.id || ids.has(seat.id) || positions.has(position) || seat.auditoriumId !== input.auditoriumId || !/^[A-Z]$/.test(seat.rowLabel) || !Number.isInteger(seat.seatNumber) || seat.seatNumber < 1 || seat.seatNumber > 30 || !['STANDARD', 'VIP', 'COUPLE'].includes(seat.seatType) || typeof seat.isActive !== 'boolean') throw new MockApiError('INVALID_SEAT', 'Ghế phải có vị trí duy nhất từ A–Z, số 1–30 và loại ghế hợp lệ.')
      if (data.seats.some(item => item.id === seat.id && item.auditoriumId !== input.auditoriumId)) throw new MockApiError('INVALID_SEAT', 'Ghế thuộc phòng chiếu khác.')
      ids.add(seat.id)
      positions.add(position)
    }
    for (const seat of seats) {
      const partner = seats.find(item => item.id === seat.pairedSeatId)
      if (seat.seatType === 'COUPLE' ? !partner || partner.id === seat.id || partner.seatType !== 'COUPLE' || partner.pairedSeatId !== seat.id || partner.rowLabel !== seat.rowLabel || Math.abs(partner.seatNumber - seat.seatNumber) !== 1 || partner.isActive !== seat.isActive : seat.pairedSeatId !== undefined) throw new MockApiError('INVALID_COUPLE', 'Ghế đôi phải tạo thành cặp 2 ghế liền nhau cùng hàng và cùng trạng thái sử dụng.')
    }
    data.seats = [...data.seats.filter(item => item.auditoriumId !== input.auditoriumId), ...seats]
    return persist(data)
  }),
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
    return persist(data)
  }),
}
