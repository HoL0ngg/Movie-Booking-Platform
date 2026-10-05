import type { Movie, MovieStatus } from '../../shared/contracts'
import { getJson } from '../../shared/apiClient'

interface MovieResponse {
  id: string
  title: string
  synopsis: string | null
  posterUrl: string | null
  durationMinutes: number
  releaseDate: string | null
  status: MovieStatus
  genres: string[]
}

export interface MovieFilters { status?: MovieStatus; query?: string; genre?: string }

// Chưa có từ backend: originalTitle, ageRating, rating, director, cast
const toMovie = (m: MovieResponse): Movie => {
  const poster = m.posterUrl?.trim() || '/movie-placeholder.svg'
  return {
    ...m,
    synopsis: m.synopsis ?? '',
    releaseDate: m.releaseDate ?? '',
    posterUrl: poster,
    backdropUrl: poster,            // tạm dùng poster làm nền hero
    genres: m.genres ?? [],
    originalTitle: '', ageRating: '', rating: null, director: '', cast: [],
  }
}

export const movieService = {
  async list(filters: MovieFilters = {}, signal?: AbortSignal) {
    const qs = new URLSearchParams()
    if (filters.status) qs.set('status', filters.status)
    if (filters.query?.trim()) qs.set('query', filters.query.trim())
    if (filters.genre) qs.set('genre', filters.genre)
    const s = qs.toString()
    const movies = await getJson<MovieResponse[]>(`/movies${s ? `?${s}` : ''}`, signal)
    return movies.map(toMovie)
  },
  async get(id: string, signal?: AbortSignal) {
    return toMovie(await getJson<MovieResponse>(`/movies/${encodeURIComponent(id)}`, signal))
  },
}