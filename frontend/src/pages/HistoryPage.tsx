import { Link } from 'react-router-dom'
import { LoadingState } from '../components/LoadingState'
import { useBookingHistory } from '../hooks/useApi'

export function HistoryPage() {
  const bookings = useBookingHistory()
  return <div className="page-width page-top history-page"><div className="page-title"><p className="eyebrow">Tài khoản</p><h1>Vé của tôi</h1><p>Tất cả hành trình điện ảnh ở một nơi.</p></div>{bookings.isLoading ? <LoadingState /> : <div className="history-list">{bookings.data?.map(booking => <article key={booking.id} className="history-card"><img src={booking.movie.posterUrl} alt="" /><div className="history-info"><div><span className={`status ${booking.status.toLowerCase()}`}>{booking.status === 'CONFIRMED' ? 'Đã xác nhận' : 'Đang xử lý'}</span><h2>{booking.movie.title}</h2><p>{booking.cinema.name}</p></div><dl><div><dt>Suất chiếu</dt><dd>{new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(booking.showtime.startsAt))}</dd></div><div><dt>Ghế</dt><dd>{booking.items.map(item => item.seatLabel).join(', ')}</dd></div><div><dt>Mã vé</dt><dd>{booking.bookingCode}</dd></div></dl>{booking.status === 'CONFIRMED' && <Link to={`/booking-success/${booking.id}`}>Xem vé →</Link>}</div></article>)}</div>}</div>
}
