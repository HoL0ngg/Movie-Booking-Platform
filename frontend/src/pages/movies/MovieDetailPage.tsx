import { Link, useParams } from 'react-router-dom'
import { ErrorState, LoadingState } from '../../components/LoadingState'
import { useMovie, useShowtimes } from '../../hooks/useApi'

const dateTime = (value: string) => new Intl.DateTimeFormat('vi-VN', { weekday: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(value))

export function MovieDetailPage() {
  const { movieId } = useParams()
  const movie = useMovie(movieId)
  const showtimes = useShowtimes(movieId)
  if (movie.isLoading) return <div className="page-width page-top"><LoadingState /></div>
  if (!movie.data) return <div className="page-width page-top"><ErrorState message="Không tìm thấy phim." /></div>
  const item = movie.data
  return <>
    <section className="detail-hero" style={{ '--backdrop': `url(${item.backdropUrl})` } as React.CSSProperties}>
      <div className="hero-shade" />
      <div className="page-width detail-layout">
        <img className="detail-poster" src={item.posterUrl} alt={`Poster ${item.title}`} />
        <div className="detail-copy"><p className="eyebrow">{item.status === 'NOW_SHOWING' ? 'Đang chiếu' : 'Sắp chiếu'}</p><h1>{item.title}</h1><p className="original-title">{item.originalTitle}</p><div className="meta"><span>★ {item.rating}</span><span>{item.durationMinutes} phút</span><span>{item.ageRating}</span></div><p className="synopsis">{item.synopsis}</p><dl><div><dt>Đạo diễn</dt><dd>{item.director}</dd></div><div><dt>Diễn viên</dt><dd>{item.cast.join(', ')}</dd></div><div><dt>Thể loại</dt><dd>{item.genres.join(', ')}</dd></div></dl>{item.status === 'NOW_SHOWING' && <Link className="primary-button" to={`/showtimes?movieId=${item.id}`}>Chọn suất chiếu →</Link>}</div>
      </div>
    </section>
    <div className="page-width detail-body">
      <section><div className="section-heading"><div><p className="eyebrow">Preview</p><h2>Trailer</h2></div></div><button className="trailer" aria-label="Phát trailer"><span>▶</span><strong>Trailer chính thức</strong><small>Video placeholder</small></button></section>
      {item.status === 'NOW_SHOWING' && <section><div className="section-heading"><div><p className="eyebrow">Gần nhất</p><h2>Suất chiếu hôm nay</h2></div></div><div className="quick-times">{showtimes.data?.slice(0, 5).map(showtime => <Link key={showtime.id} to={`/showtimes/${showtime.id}/seats`}>{dateTime(showtime.startsAt)}<small>{showtime.format}</small></Link>)}</div></section>}
    </div>
  </>
}
