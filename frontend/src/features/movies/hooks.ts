import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { movieService, type MovieFilters } from './api'
import type { MovieStatus } from '../../shared/contracts'

// Nhận 'NOW_SHOWING' (cách gọi cũ) hoặc object filter, nên ShowtimesPage không phải sửa
export const useMovies = (input?: MovieStatus | MovieFilters) => {
  const f: MovieFilters = typeof input === 'string' ? { status: input } : input ?? {}
  return useQuery({
    queryKey: ['movies', f.status ?? null, f.query ?? '', f.genre ?? ''],
    queryFn: ({ signal }) => movieService.list(f, signal),
    placeholderData: keepPreviousData,        // gõ tìm kiếm không bị nháy loading
  })
}

export const useMovie = (id = '') => useQuery({
  queryKey: ['movie', id],
  queryFn: ({ signal }) => movieService.get(id, signal),
  enabled: Boolean(id),
})