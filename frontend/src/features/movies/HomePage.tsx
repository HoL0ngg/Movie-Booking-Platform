import { Link } from 'react-router-dom'
import { MovieCard } from './MovieCard'
import { ErrorState, LoadingState } from '../../shared/LoadingState'
import { useMovies } from './hooks'
import { useMemo, useState } from 'react'
import { useDebounce } from '../../shared/useDebounce'

export function HomePage() {
  const [text, setText] = useState('')
  const [genre, setGenre] = useState('')
  const query = useDebounce(text, 300)

  const all = useMovies()                                   // không lọc, dùng để lấy danh sách thể loại
  const genres = useMemo(() => [...new Set((all.data ?? []).flatMap(m => m.genres))].sort((a, b) => a.localeCompare(b, 'vi')), [all.data])

  const now = useMovies({ status: 'NOW_SHOWING', query, genre })
  const soon = useMovies({ status: 'COMING_SOON', query, genre })
  const filtering = Boolean(query || genre)
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
    <form className="page-width movie-filter" role="search" onSubmit={e => e.preventDefault()}>
      <input type="search" value={text} maxLength={200} placeholder="Tìm phim theo tên…" aria-label="Tìm phim"
        onChange={e => setText(e.target.value)} />
      <select value={genre} aria-label="Thể loại" onChange={e => setGenre(e.target.value)}>
        <option value="">Tất cả thể loại</option>
        {genres.map(g => <option key={g} value={g}>{g}</option>)}
      </select>
      {(text || genre) && <button type="button" className="ghost-button" onClick={() => { setText(''); setGenre('') }}>Xoá lọc</button>}
    </form>
    <div className="page-width home-content">
      <section className="movie-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Đang chiếu</p>
            <h2>Trên màn ảnh</h2>
          </div>
          <Link to="/showtimes">Xem lịch chiếu →</Link>
        </div>
        {now.isLoading ? (
          <LoadingState />
        ) : now.isError ? (
          <>
            <ErrorState message="Không thể tải danh sách phim." />
            <button onClick={() => void now.refetch()}>Thử lại</button>
          </>
        ) : !now.data?.length ? (
          <p role="status">{filtering ? 'Không tìm thấy phim phù hợp.' : 'Chưa có phim đang chiếu.'}</p>
        ) : (
          <div className="movie-grid">
            {now.data.map((movie, index) => (
              <MovieCard key={movie.id} movie={movie} rank={index + 1} />
            ))}
          </div>
        )}
      </section>

      <section className="movie-section coming-soon">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Sắp ra mắt</p>
            <h2>Đón chờ</h2>
          </div>
        </div>
        {soon.isLoading ? (
          <LoadingState />
        ) : soon.isError ? (
          <>
            <ErrorState message="Không thể tải phim sắp chiếu." />
            <button onClick={() => void soon.refetch()}>Thử lại</button>
          </>
        ) : !soon.data?.length ? (
          <p role="status">{filtering ? 'Không tìm thấy phim phù hợp.' : 'Chưa có phim sắp chiếu.'}</p>
        ) : (
          <div className="movie-grid compact">
            {soon.data.map((movie) => (
              <MovieCard key={movie.id} movie={movie} />
            ))}
          </div>
        )}
      </section>
    </div>
  </>
}
