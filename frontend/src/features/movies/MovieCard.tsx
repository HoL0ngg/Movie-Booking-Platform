import { Link } from 'react-router-dom'
import type { Movie } from '../../shared/contracts'

export function MovieCard({ movie, rank }: { movie: Movie; rank?: number }) {
  return <article className="movie-card">
    <Link to={`/movies/${movie.id}`} aria-label={`Xem chi tiết ${movie.title}`}>
      <div className="poster-wrap">
        {rank && <span className="rank">{String(rank).padStart(2, '0')}</span>}
        <img src={movie.posterUrl} alt={`Poster ${movie.title}`} loading="lazy" />
        {movie.rating !== null && <span className="rating">★ {movie.rating}</span>}
      </div>
      <div className="movie-card-copy">
        <h3>{movie.title}</h3>
        <p>{movie.genres.join(' · ')}</p>
        <span>{movie.durationMinutes} phút · {movie.ageRating}</span>
      </div>
    </Link>
  </article>
}
