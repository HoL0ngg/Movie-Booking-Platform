import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useBookingDraft } from './BookingContext'
import { useCheckout, useCinemas, useSeats, useShowtime } from './hooks'
import { useMovie } from '../movies/hooks'
import type { PaymentMethod } from './api'

const money = (value: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)

export function CheckoutPage() {
  const navigate = useNavigate()
  const draft = useBookingDraft()
  const [method, setMethod] = useState<PaymentMethod>('MOMO')
  const checkout = useCheckout()
  const showtimeQuery = useShowtime(draft.showtimeId)
  const movie = useMovie(draft.movieId ?? showtimeQuery.data?.movieId)
  const seats = useSeats(draft.showtimeId)
  const cinemas = useCinemas()
  if (!draft.showtimeId || !draft.seatIds.length) return <Navigate to="/showtimes" replace />
  const showtime = showtimeQuery.data
  const cinema = cinemas.data?.find(item => item.id === showtime?.cinemaId)
  const selected = seats.data?.filter(seat => draft.seatIds.includes(seat.id)) ?? []
  const subtotal = selected.reduce((sum, seat) => sum + seat.price.amountMinor, 0)
  const submit = () => checkout.mutate({ showtimeId: draft.showtimeId!, seatIds: draft.seatIds, method }, { onSuccess: booking => { draft.clear(); navigate(`/booking-success/${booking.id}`) } })
  return <div className="page-width page-top checkout-page">
    <div className="page-title"><p className="eyebrow">Thanh toán</p><h1>Kiểm tra đơn hàng</h1><p>Ghế chỉ được xác nhận sau khi thanh toán mô phỏng hoàn tất.</p></div>
    <div className="checkout-grid">
      <section className="checkout-main">
        <h2>Phương thức thanh toán</h2>
        <div className="payment-methods">{([{ id: 'MOMO', title: 'Ví MoMo', detail: 'Thanh toán nhanh qua ví điện tử', mark: 'M' }, { id: 'VNPAY', title: 'VNPay', detail: 'QR ngân hàng nội địa', mark: 'V' }, { id: 'CARD_AT_COUNTER', title: 'Thẻ tại quầy', detail: 'Mô phỏng thanh toán tại rạp', mark: 'C' }] as const).map(item => <label className={method === item.id ? 'selected' : ''} key={item.id}><input type="radio" name="payment" value={item.id} checked={method === item.id} onChange={() => setMethod(item.id)} /><span className="payment-mark">{item.mark}</span><span><strong>{item.title}</strong><small>{item.detail}</small></span><i /></label>)}</div>
        <div className="mock-note"><strong>Chế độ prototype</strong><p>Không có giao dịch thật. Nút bên dưới mô phỏng kết quả đã được payment-service xác nhận.</p></div>
        {checkout.error && <p className="form-error" role="alert">{checkout.error.message}</p>}
      </section>
      <aside className="summary-card"><h2>Thông tin vé</h2><div className="summary-movie"><img src={movie.data?.posterUrl} alt="" /><div><strong>{movie.data?.title}</strong><span>{showtime?.format} · {movie.data?.ageRating}</span></div></div><dl><div><dt>Rạp</dt><dd>{cinema?.name}</dd></div><div><dt>Phòng</dt><dd>{showtime?.format === 'IMAX' ? 'IMAX Hall' : 'Screen 2'}</dd></div><div><dt>Suất chiếu</dt><dd>{showtime && new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(showtime.startsAt))}</dd></div><div><dt>Ghế</dt><dd>{selected.map(seat => seat.label).join(', ')}</dd></div></dl><div className="summary-total"><span>Tổng thanh toán</span><strong>{money(subtotal)}</strong></div><button className="primary-button wide" disabled={checkout.isPending || !selected.length} onClick={submit}>{checkout.isPending ? 'Đang xác nhận…' : `Thanh toán ${money(subtotal)}`}</button></aside>
    </div>
  </div>
}
