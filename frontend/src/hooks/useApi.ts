import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef } from 'react'
import { authService, bookingService, cinemaService, movieService, paymentService, seatService, type PaymentMethod } from '../api/services'

export const useMovies = (status?: 'NOW_SHOWING' | 'COMING_SOON') => useQuery({ queryKey: ['movies', status], queryFn: ({ signal }) => movieService.list(status, signal) })
export const useMovie = (id = '') => useQuery({ queryKey: ['movie', id], queryFn: ({ signal }) => movieService.get(id, signal), enabled: Boolean(id) })
export const useCinemas = () => useQuery({ queryKey: ['cinemas'], queryFn: cinemaService.list })
export const useShowtimes = (movieId = '') => useQuery({ queryKey: ['showtimes', movieId], queryFn: () => cinemaService.showtimes(movieId), enabled: Boolean(movieId) })
export const useShowtime = (id = '') => useQuery({ queryKey: ['showtime', id], queryFn: () => cinemaService.showtime(id), enabled: Boolean(id) })
export const useSeats = (showtimeId = '') => useQuery({ queryKey: ['seats', showtimeId], queryFn: () => seatService.getByShowtime(showtimeId), enabled: Boolean(showtimeId) })
export const useBooking = (id = '') => useQuery({ queryKey: ['booking', id], queryFn: () => bookingService.get(id), enabled: Boolean(id) })
export const useBookingHistory = () => useQuery({ queryKey: ['bookings'], queryFn: bookingService.history })
export const useCurrentUser = () => useQuery({ queryKey: ['current-user'], queryFn: async () => authService.current() })

export const useAuth = () => {
  const client = useQueryClient()
  const refresh = () => client.invalidateQueries({ queryKey: ['current-user'] })
  return {
    login: useMutation({ mutationFn: ({ email, password }: { email: string; password: string }) => authService.login(email, password), onSuccess: refresh }),
    register: useMutation({ mutationFn: ({ name, email, password }: { name: string; email: string; password: string }) => authService.register(name, email, password), onSuccess: refresh }),
    logout: useMutation({ mutationFn: authService.logout, onSuccess: refresh }),
  }
}

export const useCheckout = () => {
  const client = useQueryClient()
  const reservationKey = useRef(crypto.randomUUID())
  const checkoutKey = useRef(crypto.randomUUID())
  return useMutation({
    mutationFn: async ({ showtimeId, seatIds, method }: { showtimeId: string; seatIds: string[]; method: PaymentMethod }) => {
      const reservation = await bookingService.reserve(showtimeId, seatIds, reservationKey.current)
      const { payment } = await bookingService.checkout(reservation.id, method, checkoutKey.current)
      return paymentService.confirmMock(payment.id)
    },
    onSuccess: booking => {
      client.setQueryData(['booking', booking.id], booking)
      void client.invalidateQueries({ queryKey: ['bookings'] })
      void client.invalidateQueries({ queryKey: ['seats', booking.showtime.id] })
    },
  })
}
