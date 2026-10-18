import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";

// Backend giới hạn 8 ghế mỗi lần đặt (ReservationService.MAX_SEATS_PER_RESERVATION).
export const MAX_SEATS_PER_RESERVATION = 8;

interface BookingDraft {
  movieId?: string;
  showtimeId?: string;
  seatIds: string[];
  isFull: boolean;
  setMovieId: (id: string) => void;
  setShowtimeId: (id: string) => void;
  toggleSeat: (id: string) => void;
  /** Chỉ giữ lại các ghế thoả điều kiện (dùng khi ghế vừa bị người khác đặt). */
  retainSeats: (keep: (id: string) => boolean) => void;
  clear: () => void;
}

const BookingDraftContext = createContext<BookingDraft | null>(null);

export function BookingDraftProvider({ children }: { children: ReactNode }) {
  const [movieId, setMovieIdState] = useState<string>();
  const [showtimeId, setShowtimeIdState] = useState<string>();
  const [seatIds, setSeatIds] = useState<string[]>([]);
  // Chỉ reset lựa chọn khi phim/suất THỰC SỰ đổi, để quay lại từ trang thanh toán không mất ghế đã chọn.
  const movieRef = useRef<string>(undefined);
  const showtimeRef = useRef<string>(undefined);

  const setMovieId = useCallback((id: string) => {
    if (movieRef.current !== id) {
      showtimeRef.current = undefined;
      setShowtimeIdState(undefined);
      setSeatIds([]);
    }
    movieRef.current = id;
    setMovieIdState(id);
  }, []);

  const setShowtimeId = useCallback((id: string) => {
    if (showtimeRef.current !== id) setSeatIds([]);
    showtimeRef.current = id;
    setShowtimeIdState(id);
  }, []);

  const toggleSeat = useCallback(
    (id: string) =>
      setSeatIds((current) =>
        current.includes(id)
          ? current.filter((item) => item !== id)
          : current.length >= MAX_SEATS_PER_RESERVATION
            ? current
            : [...current, id],
      ),
    [],
  );

  const retainSeats = useCallback(
    (keep: (id: string) => boolean) =>
      setSeatIds((current) => {
        const next = current.filter(keep);
        return next.length === current.length ? current : next;
      }),
    [],
  );

  const clear = useCallback(() => {
    movieRef.current = undefined;
    showtimeRef.current = undefined;
    setMovieIdState(undefined);
    setShowtimeIdState(undefined);
    setSeatIds([]);
  }, []);

  const value = useMemo<BookingDraft>(
    () => ({
      movieId,
      showtimeId,
      seatIds,
      isFull: seatIds.length >= MAX_SEATS_PER_RESERVATION,
      setMovieId,
      setShowtimeId,
      toggleSeat,
      retainSeats,
      clear,
    }),
    [
      movieId,
      showtimeId,
      seatIds,
      setMovieId,
      setShowtimeId,
      toggleSeat,
      retainSeats,
      clear,
    ],
  );

  return (
    <BookingDraftContext.Provider value={value}>
      {children}
    </BookingDraftContext.Provider>
  );
}

export const useBookingDraft = () => {
  const value = useContext(BookingDraftContext);
  if (!value)
    throw new Error("useBookingDraft must be used inside BookingDraftProvider");
  return value;
};
