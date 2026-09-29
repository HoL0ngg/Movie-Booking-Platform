export type MovieStatus = 'NOW_SHOWING' | 'COMING_SOON'
export type SeatStatus = 'AVAILABLE' | 'HELD' | 'PAYMENT_PENDING' | 'SOLD'
export type SeatType = 'STANDARD' | 'VIP' | 'COUPLE'
export type BookingStatus = 'PAYMENT_PENDING' | 'CONFIRMED' | 'CANCELLED'
export type PaymentStatus = 'REQUESTED' | 'SUCCEEDED' | 'FAILED'

export interface Money { amountMinor: number; currency: 'VND' }
export interface Movie {
  id: string
  title: string
  originalTitle: string
  synopsis: string
  posterUrl: string
  backdropUrl: string
  genres: string[]
  durationMinutes: number
  ageRating: string
  releaseDate: string
  rating: number
  status: MovieStatus
  director: string
  cast: string[]
}
export interface Cinema { id: string; name: string; address: string; city: string }
export interface Auditorium { id: string; cinemaId: string; name: string }
export interface Showtime {
  id: string
  movieId: string
  cinemaId: string
  auditoriumId: string
  startsAt: string
  endsAt: string
  price: Money
  format: '2D' | 'IMAX'
  language: string
}
export interface Seat {
  id: string
  showtimeId: string
  row: string
  number: number
  label: string
  type: SeatType
  status: SeatStatus
  price: Money
}
export interface Reservation {
  id: string
  showtimeId: string
  seatIds: string[]
  status: 'HELD'
  holdExpiresAt: string
}
export interface BookingItem { id: string; seatId: string; seatLabel: string; price: Money }
export interface Booking {
  id: string
  reservationId: string
  userId: string
  movie: Pick<Movie, 'id' | 'title' | 'posterUrl'>
  cinema: Pick<Cinema, 'id' | 'name' | 'address'>
  showtime: Showtime
  items: BookingItem[]
  total: Money
  status: BookingStatus
  bookingCode: string
  createdAt: string
}
export interface Payment {
  id: string
  bookingId: string
  method: 'MOMO' | 'VNPAY' | 'CARD_AT_COUNTER'
  amount: Money
  status: PaymentStatus
}
export interface User { id: string; name: string; email: string }
export interface ApiError { code: string; message: string; traceId: string }
