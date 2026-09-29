import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'

interface BookingDraft {
  movieId?: string
  showtimeId?: string
  seatIds: string[]
  setMovieId: (id: string) => void
  setShowtimeId: (id: string) => void
  toggleSeat: (id: string) => void
  clear: () => void
}

const BookingDraftContext = createContext<BookingDraft | null>(null)

export function BookingDraftProvider({ children }: { children: ReactNode }) {
  const [movieId, setMovieIdState] = useState<string>()
  const [showtimeId, setShowtimeIdState] = useState<string>()
  const [seatIds, setSeatIds] = useState<string[]>([])
  const setMovieId = useCallback((id: string) => { setMovieIdState(id); setShowtimeIdState(undefined); setSeatIds([]) }, [])
  const setShowtimeId = useCallback((id: string) => { setShowtimeIdState(id); setSeatIds([]) }, [])
  const toggleSeat = useCallback((id: string) => setSeatIds(current => current.includes(id) ? current.filter(item => item !== id) : [...current, id]), [])
  const clear = useCallback(() => { setMovieIdState(undefined); setShowtimeIdState(undefined); setSeatIds([]) }, [])
  const value = useMemo<BookingDraft>(() => ({
    movieId, showtimeId, seatIds,
    setMovieId, setShowtimeId, toggleSeat, clear,
  }), [movieId, showtimeId, seatIds, setMovieId, setShowtimeId, toggleSeat, clear])
  return <BookingDraftContext.Provider value={value}>{children}</BookingDraftContext.Provider>
}

export const useBookingDraft = () => {
  const value = useContext(BookingDraftContext)
  if (!value) throw new Error('useBookingDraft must be used inside BookingDraftProvider')
  return value
}
