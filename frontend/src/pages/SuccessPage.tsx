import { Link, useParams } from 'react-router-dom'
import { LoadingState } from '../components/LoadingState'
import { useBooking } from '../hooks/useApi'

export function SuccessPage() {
  const { bookingId = '' } = useParams()
  const booking = useBooking(bookingId)
  if (booking.isLoading) return <div className="page-width page-top"><LoadingState label="Đang xác minh booking…" /></div>
  if (!booking.data) return <div className="page-width page-top"><p>Không tìm thấy booking.</p></div>
  const item = booking.data
  return <div className="success-page page-width page-top"><div className="success-icon">✓</div><p className="eyebrow">Thanh toán thành công</p><h1>Hẹn bạn tại rạp!</h1><p>Booking đã được xác nhận bởi mock API.</p><article className="ticket"><div className="ticket-main"><span className="ticket-brand">cinémat</span><h2>{item.movie.title}</h2><p>{item.cinema.name}</p><div className="ticket-data"><div><small>Ngày & giờ</small><strong>{new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(item.showtime.startsAt))}</strong></div><div><small>Ghế</small><strong>{item.items.map(seat => seat.seatLabel).join(', ')}</strong></div><div><small>Định dạng</small><strong>{item.showtime.format}</strong></div></div></div><div className="ticket-stub"><div className="qr-placeholder" aria-label="Mã QR vé">▦</div><small>Mã đặt vé</small><strong>{item.bookingCode}</strong></div></article><div className="button-row centered"><Link className="primary-button" to="/bookings">Xem vé của tôi</Link><Link className="secondary-button" to="/">Về trang chủ</Link></div></div>
}
