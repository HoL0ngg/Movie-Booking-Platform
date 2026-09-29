import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useBookingDraft } from '../features/booking/BookingContext'
import { useCinemas, useMovies, useShowtimes } from '../hooks/useApi'

const dayKey = (date: Date) => date.toISOString().slice(0, 10)
const days = Array.from({ length: 3 }, (_, index) => { const date = new Date(); date.setDate(date.getDate() + index); return date })

export function ShowtimesPage() {
  const [params, setParams] = useSearchParams()
  const { setMovieId, setShowtimeId } = useBookingDraft()
  const movies = useMovies('NOW_SHOWING')
  const cinemas = useCinemas()
  const movieId = params.get('movieId') ?? movies.data?.[0]?.id ?? ''
  const [cinemaId, setCinemaId] = useState<string>('all')
  const [date, setDate] = useState(dayKey(days[0]))
  const showtimes = useShowtimes(movieId)
  const filtered = useMemo(() => showtimes.data?.filter(item => (cinemaId === 'all' || item.cinemaId === cinemaId) && dayKey(new Date(item.startsAt)) === date) ?? [], [showtimes.data, cinemaId, date])
  return <div className="page-width page-top showtimes-page">
    <div className="page-title"><p className="eyebrow">Lịch chiếu</p><h1>Chọn trải nghiệm của bạn</h1><p>Chọn phim, ngày và rạp phù hợp.</p></div>
    <div className="filter-card">
      <label>Phim<select value={movieId} onChange={event => { setParams({ movieId: event.target.value }); setMovieId(event.target.value) }}>{movies.data?.map(movie => <option key={movie.id} value={movie.id}>{movie.title}</option>)}</select></label>
      <div><span className="field-label">Ngày xem</span><div className="date-tabs">{days.map((item, index) => <button key={dayKey(item)} className={date === dayKey(item) ? 'active' : ''} onClick={() => setDate(dayKey(item))}><small>{index === 0 ? 'Hôm nay' : new Intl.DateTimeFormat('vi-VN', { weekday: 'short' }).format(item)}</small><strong>{item.getDate()}</strong><span>Tháng {item.getMonth() + 1}</span></button>)}</div></div>
      <label>Rạp<select value={cinemaId} onChange={event => setCinemaId(event.target.value)}><option value="all">Tất cả rạp</option>{cinemas.data?.map(cinema => <option key={cinema.id} value={cinema.id}>{cinema.name}</option>)}</select></label>
    </div>
    <div className="cinema-list">{cinemas.data?.filter(cinema => cinemaId === 'all' || cinema.id === cinemaId).map(cinema => { const times = filtered.filter(item => item.cinemaId === cinema.id); return <article className="cinema-row" key={cinema.id}><div><span className="cinema-dot" /><h2>{cinema.name}</h2><p>{cinema.address}</p></div><div className="time-grid">{times.length ? times.map(showtime => <Link key={showtime.id} to={`/showtimes/${showtime.id}/seats`} onClick={() => { setMovieId(showtime.movieId); setShowtimeId(showtime.id) }}><strong>{new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date(showtime.startsAt))}</strong><span>{showtime.format}</span></Link>) : <p className="muted">Không có suất chiếu trong ngày này.</p>}</div></article> })}</div>
  </div>
}
