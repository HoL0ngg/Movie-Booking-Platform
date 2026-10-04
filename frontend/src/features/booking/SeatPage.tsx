import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ErrorState, LoadingState } from '../../shared/LoadingState'
import { useBookingDraft } from './BookingContext'
import { useCinemas, useSeats, useShowtime } from './hooks'
import { useMovie } from '../movies/hooks'
import type { Seat } from '../../shared/contracts'

const money = (value: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)

export function SeatPage() {
  const { showtimeId = '' } = useParams()
  const { seatIds, toggleSeat, setMovieId, setShowtimeId } = useBookingDraft()
  const seats = useSeats(showtimeId)
  const showtime = useShowtime(showtimeId)
  const movie = useMovie(showtime.data?.movieId)
  const cinemas = useCinemas()
  useEffect(() => { if (showtime.data?.movieId) { setMovieId(showtime.data.movieId); setShowtimeId(showtimeId) } }, [showtime.data?.movieId, showtimeId, setMovieId, setShowtimeId])
  if (seats.isLoading || movie.isLoading) return <div className="page-width page-top"><LoadingState label="Đang tải sơ đồ ghế…" /></div>
  if (!seats.data?.length) return <div className="page-width page-top"><ErrorState message="Không tìm thấy sơ đồ ghế." /></div>
  const selected = seats.data.filter(seat => seatIds.includes(seat.id))
  const total = selected.reduce((sum, seat) => sum + seat.price.amountMinor, 0)
  const grouped = seats.data.reduce<Record<string, Seat[]>>((rows, seat) => {
    (rows[seat.row] ??= []).push(seat)
    return rows
  }, {})
  const selectedShowtime = showtime.data
  const cinema = cinemas.data?.find(item => item.id === selectedShowtime?.cinemaId)
  return <div className="page-width page-top seat-page">
    <div className="booking-heading"><div><Link className="back-link" to={`/showtimes?movieId=${movie.data?.id}`}>← Đổi suất chiếu</Link><p className="eyebrow">Chọn ghế</p><h1>{movie.data?.title}</h1><p>{cinema?.name ?? 'Cinémat'} · {selectedShowtime ? new Intl.DateTimeFormat('vi-VN', { weekday: 'long', hour: '2-digit', minute: '2-digit' }).format(new Date(selectedShowtime.startsAt)) : 'Suất chiếu đã chọn'}</p></div><div className="stepper"><span className="done">1</span><i /><span className="active">2</span><i /><span>3</span></div></div>
    <div className="seat-layout-card">
      <div className="screen"><span>Màn hình</span></div>
      <div className="seat-map" role="group" aria-label="Sơ đồ ghế">{Object.entries(grouped).map(([row, rowSeats]) => <div className="seat-row" key={row}><b>{row}</b><div>{rowSeats!.map((seat: Seat) => { const unavailable = seat.status !== 'AVAILABLE'; const active = seatIds.includes(seat.id); return <button key={seat.id} className={`seat ${seat.type.toLowerCase()} ${active ? 'selected' : ''} ${unavailable ? 'unavailable' : ''}`} disabled={unavailable} aria-pressed={active} aria-label={`${seat.label}, ghế ${seat.type === 'VIP' ? 'VIP' : 'thường'}, ${unavailable ? 'không khả dụng' : active ? 'đã chọn' : 'còn trống'}`} onClick={() => toggleSeat(seat.id)}><span>{seat.number}</span></button> })}</div><b>{row}</b></div>)}</div>
      <div className="seat-legend"><span><i className="seat-demo" />Còn trống</span><span><i className="seat-demo selected" />Đang chọn</span><span><i className="seat-demo unavailable" />Đã đặt</span><span><i className="seat-demo vip" />VIP</span></div>
    </div>
    <aside className="booking-bar"><div><small>Ghế đã chọn</small><strong>{selected.length ? selected.map(seat => seat.label).join(', ') : 'Chưa chọn ghế'}</strong></div><div><small>Tổng cộng</small><strong className="price">{money(total)}</strong></div>{selected.length ? <Link className="primary-button" to="/checkout">Tiếp tục →</Link> : <button className="primary-button" disabled>Chọn ghế để tiếp tục</button>}</aside>
  </div>
}
