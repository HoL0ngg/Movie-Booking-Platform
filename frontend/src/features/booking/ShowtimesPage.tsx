import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useBookingDraft } from './BookingContext'
import { useCinemas, useShowtimes } from './hooks'
import { useMovie, useMovies } from '../movies/hooks'
import { MovieCard } from '../movies/MovieCard'
import { ErrorState, LoadingState } from '../../shared/LoadingState'
import './showtime-filters.css'

const dayKey = (date: Date) => date.toISOString().slice(0, 10)

function MovieSelection() {
  const movies = useMovies('NOW_SHOWING')
  return <div className="page-width page-top showtimes-page">
    <div className="page-title"><p className="eyebrow">Lịch chiếu</p><h1>Bạn muốn xem phim gì?</h1></div>
    {movies.isPending ? <LoadingState /> : movies.isError ? <><ErrorState message="Không thể tải danh sách phim." /><button className="secondary-button" onClick={() => void movies.refetch()}>Thử lại</button></> : movies.data.length ? <div className="movie-grid">{movies.data.map(movie => <MovieCard key={movie.id} movie={movie} />)}</div> : <p role="status">Chưa có phim đang chiếu.</p>}
  </div>
}

function MovieShowtimes({ movieId }: { movieId: string }) {
  const { setMovieId, setShowtimeId } = useBookingDraft()
  const movie = useMovie(movieId)
  const cinemas = useCinemas()
  const showtimes = useShowtimes(movieId)
  const [days] = useState(() => Array.from({ length: 3 }, (_, index) => { const date = new Date(); date.setDate(date.getDate() + index); return date }))
  const [date, setDate] = useState(dayKey(days[0]))
  const filtered = useMemo(() => showtimes.data?.filter(item => dayKey(new Date(item.startsAt)) === date) ?? [], [showtimes.data, date])

  if (movie.isPending) return <div className="page-width page-top"><LoadingState /></div>
  if (movie.isError || !movie.data) return <div className="page-width page-top"><ErrorState message="Không thể tải thông tin phim." /><button className="secondary-button" onClick={() => void movie.refetch()}>Thử lại</button><Link className="ghost-button" to="/showtimes">Chọn phim khác</Link></div>

  const item = movie.data
  return <div className="page-width page-top showtimes-page">
    <Link className="showtime-back" to="/showtimes">← Chọn phim khác</Link>
    <header className="showtime-movie-header">
      <img src={item.posterUrl} alt={`Poster ${item.title}`} />
      <div>
        <h1>{item.title}</h1>
        {item.originalTitle && <p className="muted">{item.originalTitle}</p>}
        <p className="muted">{[`${item.durationMinutes} phút`, item.ageRating, ...item.genres, item.rating !== null ? `★ ${item.rating}` : ''].filter(Boolean).join(' · ')}</p>
        {(item.releaseDate || item.director || item.cast.length > 0) && <dl className="showtime-movie-facts">
          {item.releaseDate && <div><dt>Khởi chiếu</dt><dd><time dateTime={item.releaseDate}>{new Intl.DateTimeFormat('vi-VN', { timeZone: 'UTC' }).format(new Date(item.releaseDate))}</time></dd></div>}
          {item.director && <div><dt>Đạo diễn</dt><dd>{item.director}</dd></div>}
          {item.cast.length > 0 && <div><dt>Diễn viên</dt><dd>{item.cast.join(', ')}</dd></div>}
        </dl>}
      </div>
    </header>
    {item.synopsis && <p className="showtime-movie-synopsis">{item.synopsis}</p>}
    <section className="showtime-filters" aria-label="Chọn ngày xem">
      <div className="showtime-filter-dates">
        <h2 id="showtime-date-label">Lịch chiếu</h2>
        <div className="showtime-date-strip" role="group" aria-labelledby="showtime-date-label">{days.map((item, index) => <button type="button" key={dayKey(item)} aria-pressed={date === dayKey(item)} onClick={() => setDate(dayKey(item))}>
          <span>{index === 0 ? 'Hôm nay' : index === 1 ? 'Ngày mai' : new Intl.DateTimeFormat('vi-VN', { weekday: 'long' }).format(item)}</span>
          <strong>{new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }).format(item)}</strong>
        </button>)}</div>
      </div>
    </section>
    {cinemas.isPending || showtimes.isPending ? <LoadingState label="Đang tải lịch chiếu…" /> : cinemas.isError || showtimes.isError ? <><ErrorState message="Không thể tải lịch chiếu." /><button className="secondary-button" onClick={() => { void cinemas.refetch(); void showtimes.refetch() }}>Thử lại</button></> : !cinemas.data.length ? <p role="status">Chưa có rạp chiếu phim.</p> :
      <div className="cinema-list">{cinemas.data.map(cinema => { const times = filtered.filter(item => item.cinemaId === cinema.id); return <article className="cinema-row" key={cinema.id}><div><span className="cinema-dot" /><h2>{cinema.name}</h2><p>{cinema.address}</p></div><div className="time-grid">{times.length ? times.map(showtime => <Link key={showtime.id} to={`/showtimes/${showtime.id}/seats`} onClick={() => { setMovieId(showtime.movieId); setShowtimeId(showtime.id) }}><strong>{new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date(showtime.startsAt))}</strong><span>{showtime.format}</span></Link>) : <p className="muted">Không có suất chiếu trong ngày này.</p>}</div></article> })}</div>}
  </div>
}

export function ShowtimesPage() {
  const [params] = useSearchParams()
  const movieId = params.get('movieId')
  return movieId ? <MovieShowtimes key={movieId} movieId={movieId} /> : <MovieSelection />
}
