import { mockApi } from '../mocks/api'
import type { Movie, MovieStatus, Payment } from '../types/contracts'
import { getJson } from './client'

interface MovieResponse {
  id: string
  title: string
  synopsis: string | null
  durationMinutes: number
  releaseDate: string | null
  status: MovieStatus
}

// The current catalog schema does not contain artwork, credits or ratings.
const toMovie = (movie: MovieResponse): Movie => ({
  ...movie, synopsis: movie.synopsis ?? '', releaseDate: movie.releaseDate ?? '',
  originalTitle: '', posterUrl: '/movie-placeholder.svg', backdropUrl: '',
  genres: [], ageRating: '', rating: null, director: '', cast: [],
})

export const movieService = {
  async list(status?: MovieStatus, signal?: AbortSignal) {
    const movies = await getJson<MovieResponse[]>(`/movies${status ? `?status=${encodeURIComponent(status)}` : ''}`, signal)
    return movies.map(toMovie)
  },
  async get(id: string, signal?: AbortSignal) {
    return toMovie(await getJson<MovieResponse>(`/movies/${encodeURIComponent(id)}`, signal))
  },
}
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
