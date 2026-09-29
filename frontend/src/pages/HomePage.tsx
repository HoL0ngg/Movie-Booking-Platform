import { Link } from 'react-router-dom'
import { MovieCard } from '../components/MovieCard'
import { LoadingState } from '../components/LoadingState'
import { useMovies } from '../hooks/useApi'

export function HomePage() {
  const now = useMovies('NOW_SHOWING')
  const soon = useMovies('COMING_SOON')
  const featured = now.data?.[0]
  return <>
    <section className="hero" style={featured ? { '--backdrop': `url(${featured.backdropUrl})` } as React.CSSProperties : undefined}>
      <div className="hero-shade" />
      <div className="hero-content page-width">
        <p className="eyebrow">Phim nổi bật tuần này</p>
        <h1>{featured?.title ?? 'Mỗi bộ phim, một thế giới mới.'}</h1>
        <p>{featured?.synopsis ?? 'Khám phá những câu chuyện đáng nhớ trên màn ảnh lớn.'}</p>
        {featured && <div className="button-row"><Link className="primary-button" to={`/showtimes?movieId=${featured.id}`}>Đặt vé ngay <span>→</span></Link><Link className="secondary-button" to={`/movies/${featured.id}`}>Chi tiết phim</Link></div>}
      </div>
      <div className="hero-index">01 <span>/ 03</span></div>
    </section>
    <div className="page-width home-content">
      <section className="movie-section">
        <div className="section-heading"><div><p className="eyebrow">Đang chiếu</p><h2>Trên màn ảnh</h2></div><Link to="/showtimes">Xem lịch chiếu →</Link></div>
        {now.isLoading ? <LoadingState /> : <div className="movie-grid">{now.data?.map((movie, index) => <MovieCard key={movie.id} movie={movie} rank={index + 1} />)}</div>}
      </section>
      <section className="movie-section coming-soon">
        <div className="section-heading"><div><p className="eyebrow">Sắp ra mắt</p><h2>Đón chờ</h2></div></div>
        {soon.isLoading ? <LoadingState /> : <div className="movie-grid compact">{soon.data?.map(movie => <MovieCard key={movie.id} movie={movie} />)}</div>}
      </section>
    </div>
  </>
}
