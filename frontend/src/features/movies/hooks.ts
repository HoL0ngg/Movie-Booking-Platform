import { useQuery } from '@tanstack/react-query'
import { movieService } from './api'

export const useMovies = (status?: 'NOW_SHOWING' | 'COMING_SOON') => useQuery({ queryKey: ['movies', status], queryFn: ({ signal }) => movieService.list(status, signal) })
export const useMovie = (id = '') => useQuery({ queryKey: ['movie', id], queryFn: ({ signal }) => movieService.get(id, signal), enabled: Boolean(id) })
