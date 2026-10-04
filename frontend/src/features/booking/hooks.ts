import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef } from 'react'
import { bookingService, cinemaService, paymentService, seatService, type PaymentMethod } from './api'

export const useCinemas = () => useQuery({ queryKey: ['cinemas'], queryFn: cinemaService.list })
export const useShowtimes = (movieId = '') => useQuery({ queryKey: ['showtimes', movieId], queryFn: () => cinemaService.showtimes(movieId), enabled: Boolean(movieId) })
export const useShowtime = (id = '') => useQuery({ queryKey: ['showtime', id], queryFn: () => cinemaService.showtime(id), enabled: Boolean(id) })
export const useSeats = (showtimeId = '') => useQuery({ queryKey: ['seats', showtimeId], queryFn: () => seatService.getByShowtime(showtimeId), enabled: Boolean(showtimeId) })
export const useBooking = (id = '') => useQuery({ queryKey: ['booking', id], queryFn: () => bookingService.get(id), enabled: Boolean(id) })
export const useBookingHistory = () => useQuery({ queryKey: ['bookings'], queryFn: bookingService.history })

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
