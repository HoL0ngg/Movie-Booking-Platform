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
}

// Credits and ratings remain unavailable in the catalog response.
const toMovie = (movie: MovieResponse): Movie => ({
  ...movie, synopsis: movie.synopsis ?? '', releaseDate: movie.releaseDate ?? '',
  originalTitle: '', posterUrl: movie.posterUrl?.trim() || '/movie-placeholder.svg', backdropUrl: '',
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
