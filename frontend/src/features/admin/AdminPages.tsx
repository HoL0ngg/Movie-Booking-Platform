import { useState } from 'react'
import { Link, useOutletContext, useSearchParams } from 'react-router-dom'
import type { Booking, Cinema } from '../../shared/contracts'
import type { AdminData, AdminPayment, CatalogKind, CatalogRecord } from './adminService'
import { CatalogEditor } from './CatalogEditor'
import { RoomManager } from './RoomManager'
import { dateTime, Empty, Icon, Modal, money, PageHeading, Status } from './AdminUI'

export function AdminOverview() {
  const data = useOutletContext<AdminData>()
  const succeeded = data.payments.filter(item => item.status === 'SUCCEEDED')
  const revenue = succeeded.reduce((total, item) => total + item.amount.amountMinor, 0)
  const failed = data.payments.filter(item => item.status === 'FAILED').length
  const days = Array.from({ length: 7 }, (_, index) => { const day = new Date(); day.setDate(day.getDate() - 6 + index); return day })
  const daily = days.map(day => succeeded.filter(item => new Date(item.createdAt).toDateString() === day.toDateString()).reduce((sum, item) => sum + item.amount.amountMinor, 0))
  const maximum = Math.max(...daily, 1)
  const topMovies = data.movies.map(movie => ({ movie, tickets: data.bookings.filter(item => item.movie.id === movie.id && item.status === 'CONFIRMED').reduce((sum, item) => sum + item.items.length, 0) })).sort((a, b) => b.tickets - a.tickets).slice(0, 3)
  return <>
    <PageHeading title="Tổng quan vận hành" description="Một góc nhìn rõ ràng về hoạt động của hệ thống rạp."><Link className="admin-button" to="/admin/showtimes"><Icon name="showtimes" />Xem lịch chiếu</Link><Link className="admin-button primary" to="/admin/movies"><Icon name="plus" />Quản lý phim</Link></PageHeading>
    <div className="admin-metrics">
      <Metric label="Doanh thu demo" value={money(revenue)} note="Từ các giao dịch thành công" icon="payments" />
      <Metric label="Đơn đặt vé" value={String(data.bookings.length)} note={`${data.bookings.filter(item => item.status === 'CONFIRMED').length} đơn đã xác nhận`} icon="bookings" />
      <Metric label="Phim đang chiếu" value={String(data.movies.filter(item => item.status === 'NOW_SHOWING').length).padStart(2, '0')} note={`${data.movies.length} phim trong danh mục`} icon="movies" />
      <Metric label="Rạp chiếu phim" value={String(data.cinemas.length).padStart(2, '0')} note={`${data.auditoriums.length} phòng chiếu trong hệ thống`} icon="cinemas" />
    </div>
    <div className="admin-overview-grid">
      <section className="admin-panel"><div className="admin-panel-heading"><div><h2>Doanh thu theo ngày</h2><p>Giao dịch demo thành công trong 7 ngày gần nhất</p></div><span className="admin-tag">7 ngày</span></div><div className="admin-chart-total">{money(daily.reduce((sum, amount) => sum + amount, 0))}<span>Tổng doanh thu trong kỳ</span></div><div className="admin-chart" role="img" aria-label={days.map((day, index) => `${day.toLocaleDateString('vi-VN')}: ${money(daily[index])}`).join('; ')}><div className="admin-chart-grid"><span>{money(maximum)}</span><span>{money(maximum / 2)}</span><span>0 ₫</span></div><div className="admin-chart-bars">{days.map((day, index) => <div className="admin-chart-column" key={index}><div className="admin-chart-bar-track"><div className={`admin-chart-bar ${index === 6 ? 'current' : ''}`} style={{ height: `${daily[index] / maximum * 100}%` }} title={money(daily[index])} /></div><span>{day.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })}</span></div>)}</div></div><div className="admin-chart-legend"><span />Thanh toán thành công · VND</div></section>
      <section className="admin-panel"><div className="admin-panel-heading"><div><h2>Phim nổi bật</h2><p>Theo số vé đã xác nhận trong dữ liệu demo</p></div><Icon name="movies" /></div><div className="admin-top-movies">{topMovies.map(({ movie, tickets }, index) => <div className="admin-top-movie" key={movie.id}><span className="admin-rank">0{index + 1}</span><div className="admin-film-mark"><Icon name="movies" /></div><div><strong>{movie.title}</strong><small>{movie.genres[0]}</small></div><span className="admin-ticket-count">{tickets}<small>vé</small></span></div>)}</div><div className="admin-insight"><Icon name="payments" /><div><strong>{failed} giao dịch cần theo dõi</strong><p>Các giao dịch demo ghi nhận thất bại.</p><Link to="/admin/payments?status=FAILED">Xem giao dịch <Icon name="arrow" /></Link></div></div></section>
    </div>
    <section className="admin-panel"><div className="admin-panel-heading"><div><h2>Giao dịch gần đây</h2><p>Theo dõi tình trạng thanh toán của từng đơn</p></div><Link className="admin-text-link" to="/admin/payments">Xem tất cả <Icon name="arrow" /></Link></div><div className="admin-table-scroll"><table className="admin-table"><thead><tr><th>Mã giao dịch</th><th>Đơn đặt vé</th><th>Phương thức</th><th>Thời gian</th><th>Số tiền</th><th>Trạng thái</th></tr></thead><tbody>{[...data.payments].sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt)).slice(0, 5).map(item => <tr key={item.id}><td><span className="admin-mono">{item.reference}</span></td><td>{data.bookings.find(booking => booking.id === item.bookingId)?.bookingCode}</td><td>{item.method}</td><td className="admin-cell-muted">{dateTime(item.createdAt)}</td><td className="admin-cell-strong">{money(item.amount.amountMinor)}</td><td><Status value={item.status} /></td></tr>)}</tbody></table></div></section>
  </>
}
function Metric({ label, value, note, icon }: { label: string; value: string; note: string; icon: 'payments' | 'bookings' | 'movies' | 'cinemas' }) { return <section className="admin-metric"><div><span>{label}</span><Icon name={icon} /></div><strong>{value}</strong><p>{note}</p></section> }

const pageInfo = {
  movies: { title: 'Quản lý phim', description: 'Danh mục phim, thông tin phát hành và trạng thái trình chiếu.', add: 'Thêm phim' },
  cinemas: { title: 'Rạp chiếu phim', description: 'Quản lý hệ thống rạp, địa chỉ và thông tin phòng chiếu.', add: 'Thêm rạp' },
  showtimes: { title: 'Lịch chiếu', description: 'Sắp xếp suất chiếu theo phim, rạp và phòng chiếu.', add: 'Thêm suất chiếu' },
  bookings: { title: 'Quản lý đặt vé', description: 'Tra cứu đơn đặt vé, ghế ngồi và trạng thái xác nhận.', add: '' },
  payments: { title: 'Quản lý thanh toán', description: 'Theo dõi giao dịch, phương thức và kết quả thanh toán.', add: '' },
}
type Section = keyof typeof pageInfo
export function AdminRecords({ section }: { section: Section }) {
  const data = useOutletContext<AdminData>()
  const [searchParams, setSearchParams] = useSearchParams()
  const [search, setSearch] = useState('')
  const filter = searchParams.get('status') ?? ''
  const [page, setPage] = useState(1)
  const [editor, setEditor] = useState<{ record?: CatalogRecord } | null>(null)
  const [details, setDetails] = useState<Booking | AdminPayment | null>(null)
  const [notice, setNotice] = useState('')
  const [managedCinema, setManagedCinema] = useState<Cinema | null>(null)
  const info = pageInfo[section]
  const term = search.trim().toLocaleLowerCase('vi-VN')
  const movieName = (id: string) => data.movies.find(item => item.id === id)?.title ?? id
  const cinemaName = (id: string) => data.cinemas.find(item => item.id === id)?.name ?? id
  const match = (...values: string[]) => values.join(' ').toLocaleLowerCase('vi-VN').includes(term)
  const movies = data.movies.filter(item => match(item.title, item.originalTitle, item.director, item.genres.join(' ')) && (!filter || item.status === filter))
  const cinemas = data.cinemas.filter(item => match(item.name, item.address, item.city) && (!filter || item.city === filter))
  const showtimes = [...data.showtimes].sort((a, b) => Date.parse(a.startsAt) - Date.parse(b.startsAt)).filter(item => match(movieName(item.movieId), cinemaName(item.cinemaId), item.format) && (!filter || item.cinemaId === filter))
  const bookings = [...data.bookings].sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt)).filter(item => match(item.bookingCode, item.movie.title, item.cinema.name) && (!filter || item.status === filter))
  const payments = [...data.payments].sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt)).filter(item => match(item.reference, item.method, data.bookings.find(booking => booking.id === item.bookingId)?.bookingCode ?? '') && (!filter || item.status === filter))
  const count = { movies, cinemas, showtimes, bookings, payments }[section].length
  const pages = Math.max(1, Math.ceil(count / 8))
  const currentPage = Math.min(page, pages)
  const slice = <T,>(items: T[]) => items.slice((currentPage - 1) * 8, currentPage * 8)
  const filters = section === 'movies' ? [['NOW_SHOWING', 'Đang chiếu'], ['COMING_SOON', 'Sắp chiếu']] : section === 'cinemas' ? [...new Set(data.cinemas.map(item => item.city))].map(city => [city, city]) : section === 'showtimes' ? data.cinemas.map(item => [item.id, item.name]) : section === 'bookings' ? [['CONFIRMED', 'Đã xác nhận'], ['PAYMENT_PENDING', 'Chờ thanh toán'], ['CANCELLED', 'Đã hủy']] : [['SUCCEEDED', 'Thành công'], ['REQUESTED', 'Đang xử lý'], ['FAILED', 'Thất bại']]
  const editButton = (record: CatalogRecord) => <button className="admin-icon-button" aria-label={`Chỉnh sửa ${'title' in record ? record.title : 'name' in record ? record.name : movieName(record.movieId)}`} onClick={() => setEditor({ record })}><Icon name="edit" /></button>
  return <>
    <PageHeading title={info.title} description={info.description}>{info.add && <button className="admin-button primary" onClick={() => setEditor({})}><Icon name="plus" />{info.add}</button>}</PageHeading>
    {notice && <div className="admin-success-notice" role="status">{notice}<button className="admin-icon-button" aria-label="Đóng thông báo" onClick={() => setNotice('')}><Icon name="close" /></button></div>}
    {(section === 'bookings' || section === 'payments') && <div className="admin-summary-strip"><div><span>Tổng {section === 'payments' ? 'giao dịch' : 'đơn đặt vé'}</span><strong>{data[section].length}</strong></div><div><span>{section === 'payments' ? 'Thanh toán thành công' : 'Đã xác nhận'}</span><strong>{section === 'payments' ? data.payments.filter(item => item.status === 'SUCCEEDED').length : data.bookings.filter(item => item.status === 'CONFIRMED').length}</strong></div><div><span>Chờ thanh toán / xử lý</span><strong>{section === 'payments' ? data.payments.filter(item => item.status === 'REQUESTED').length : data.bookings.filter(item => item.status === 'PAYMENT_PENDING').length}</strong></div></div>}
    <section className={`admin-panel${section === 'cinemas' ? ' admin-cinema-directory' : ''}`}>
      <div className="admin-record-toolbar"><div className="admin-record-title"><h2>Danh sách {section === 'movies' ? 'phim' : section === 'cinemas' ? 'rạp' : section === 'showtimes' ? 'suất chiếu' : section === 'bookings' ? 'đặt vé' : 'giao dịch'}</h2><span className="admin-count">{count}</span></div><div className="admin-filters"><label className="admin-search"><Icon name="search" /><input aria-label="Tìm kiếm danh sách" type="search" placeholder="Tìm kiếm…" value={search} onChange={event => { setSearch(event.target.value); setPage(1) }} /></label><select aria-label="Lọc danh sách" value={filter} onChange={event => { setPage(1); setSearchParams(event.target.value ? { status: event.target.value } : {}, { replace: true }) }}><option value="">{section === 'cinemas' ? 'Tất cả thành phố' : section === 'showtimes' ? 'Tất cả rạp' : 'Tất cả trạng thái'}</option>{filters.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div></div>
      {!count ? <Empty /> : section === 'cinemas' ? <div className="admin-cinema-grid">{slice(cinemas).map(cinema => {
        const rooms = data.auditoriums.filter(item => item.cinemaId === cinema.id)
        const slots = data.showtimes.filter(item => item.cinemaId === cinema.id)
        return <article className="admin-cinema-card" key={cinema.id}>
          <div className="admin-cinema-card-top"><span className="admin-cinema-icon"><Icon name="cinemas" /></span><span className="admin-cinema-city">{cinema.city}</span></div>
          <div className="admin-cinema-card-heading"><h3>{cinema.name}</h3><p>{cinema.address}</p></div>
          <dl className="admin-cinema-stats"><div><dt>Phòng chiếu</dt><dd>{rooms.length}</dd></div><div><dt>Suất chiếu</dt><dd>{slots.length}</dd></div></dl>
          <div className="admin-cinema-room-list"><span className="admin-cinema-room-label">Phòng chiếu</span><div className="admin-cinema-rooms">{rooms.length ? rooms.map(room => <span key={room.id}>{room.name}</span>) : <span>Chưa có phòng chiếu</span>}</div></div>
          <div className="admin-cinema-card-bottom"><div className="admin-room-actions"><button className="admin-button" aria-label={`Chỉnh sửa ${cinema.name}`} onClick={() => setEditor({ record: cinema })}><Icon name="edit" />Chỉnh sửa</button><button className="admin-button primary" aria-label={`Quản lý phòng ${cinema.name}`} onClick={() => setManagedCinema(cinema)}><Icon name="cinemas" />Quản lý phòng</button></div><Link aria-label={`Xem lịch chiếu ${cinema.name}`} to={`/admin/showtimes?status=${cinema.id}`}>Xem lịch chiếu <Icon name="arrow" /></Link></div>
        </article>
      })}</div> : <div className="admin-table-scroll"><table className="admin-table"><thead><tr>
        {section === 'movies' && <><th>Phim</th><th>Thể loại</th><th>Thời lượng</th><th>Khởi chiếu</th><th>Trạng thái</th><th><span className="admin-sr-only">Thao tác</span></th></>}
        {section === 'showtimes' && <><th>Phim / Rạp</th><th>Phòng chiếu</th><th>Bắt đầu</th><th>Kết thúc</th><th>Giá vé</th><th>Định dạng</th><th><span className="admin-sr-only">Thao tác</span></th></>}
        {section === 'bookings' && <><th>Mã đặt vé</th><th>Phim / Rạp</th><th>Ghế</th><th>Tổng tiền</th><th>Trạng thái</th><th><span className="admin-sr-only">Chi tiết</span></th></>}
        {section === 'payments' && <><th>Mã giao dịch</th><th>Đơn đặt vé</th><th>Phương thức</th><th>Số tiền</th><th>Trạng thái</th><th><span className="admin-sr-only">Chi tiết</span></th></>}
      </tr></thead><tbody>
        {section === 'movies' && slice(movies).map(movie => <tr key={movie.id}><td><div className="admin-movie-cell"><div className="admin-film-mark"><Icon name="movies" /></div><div><strong>{movie.title}</strong><small>{movie.originalTitle || movie.director}</small></div></div></td><td className="admin-cell-muted">{movie.genres.join(', ') || '—'}</td><td>{movie.durationMinutes} phút</td><td>{new Date(`${movie.releaseDate}T00:00:00`).toLocaleDateString('vi-VN')}</td><td><Status value={movie.status} /></td><td>{editButton(movie)}</td></tr>)}
        {section === 'showtimes' && slice(showtimes).map(slot => <tr key={slot.id}><td><div className="admin-stacked-cell"><strong>{movieName(slot.movieId)}</strong><small>{cinemaName(slot.cinemaId)}</small></div></td><td>{data.auditoriums.find(item => item.id === slot.auditoriumId)?.name ?? '—'}</td><td>{dateTime(slot.startsAt)}</td><td>{dateTime(slot.endsAt)}</td><td className="admin-cell-strong">{money(slot.price.amountMinor)}</td><td><span className="admin-tag">{slot.format}</span></td><td>{data.bookings.some(item => item.showtime.id === slot.id) ? <span className="admin-cell-muted" title="Không chỉnh sửa suất chiếu đã có đơn demo">Đã có vé</span> : editButton(slot)}</td></tr>)}
        {section === 'bookings' && slice(bookings).map(booking => <tr key={booking.id}><td><span className="admin-mono">{booking.bookingCode}</span><small className="admin-table-date">{dateTime(booking.createdAt)}</small></td><td><div className="admin-stacked-cell"><strong>{booking.movie.title}</strong><small>{booking.cinema.name}</small></div></td><td>{booking.items.map(item => item.seatLabel).join(', ')}</td><td className="admin-cell-strong">{money(booking.total.amountMinor)}</td><td><Status value={booking.status} /></td><td><button className="admin-table-link" aria-label={`Chi tiết đơn ${booking.bookingCode}`} onClick={() => setDetails(booking)}>Chi tiết</button></td></tr>)}
        {section === 'payments' && slice(payments).map(payment => <tr key={payment.id}><td><span className="admin-mono">{payment.reference}</span><small className="admin-table-date">{dateTime(payment.createdAt)}</small></td><td>{data.bookings.find(item => item.id === payment.bookingId)?.bookingCode ?? '—'}</td><td>{payment.method}</td><td className="admin-cell-strong">{money(payment.amount.amountMinor)}</td><td><Status value={payment.status} /></td><td><button className="admin-table-link" aria-label={`Chi tiết giao dịch ${payment.reference}`} onClick={() => setDetails(payment)}>Chi tiết</button></td></tr>)}
      </tbody></table></div>}
      <div className="admin-pagination"><span>{count ? `Hiển thị ${(currentPage - 1) * 8 + 1}–${Math.min(currentPage * 8, count)} trong ${count} kết quả` : '0 kết quả'}</span><div><button className="admin-button" disabled={currentPage <= 1} onClick={() => setPage(currentPage - 1)}>Trước</button><span>Trang {currentPage} / {pages}</span><button className="admin-button" disabled={currentPage >= pages} onClick={() => setPage(currentPage + 1)}>Sau</button></div></div>
    </section>
    {(section === 'payments' || section === 'bookings') && <p className="admin-readonly-note">Chỉ xem dữ liệu demo. Xác nhận thanh toán, hủy vé và hoàn tiền cần API cùng quyền quản trị từ backend.</p>}
    {editor && <CatalogEditor kind={section as CatalogKind} record={editor.record} data={data} onClose={() => setEditor(null)} onSaved={() => setNotice('Đã lưu thay đổi trong không gian demo.')} />}
    {details && <RecordDetails record={details} data={data} onClose={() => setDetails(null)} />}
    {managedCinema && <RoomManager cinema={managedCinema} data={data} onClose={() => setManagedCinema(null)} />}
  </>
}

function RecordDetails({ record, data, onClose }: { record: Booking | AdminPayment; data: AdminData; onClose: () => void }) {
  const isPayment = 'reference' in record
  const booking = isPayment ? data.bookings.find(item => item.id === record.bookingId) : record
  return <Modal title={isPayment ? 'Chi tiết giao dịch' : 'Chi tiết đặt vé'} onClose={onClose}><div className="admin-detail-body"><div className="admin-detail-summary"><div><span className="admin-overline">{isPayment ? record.reference : record.bookingCode}</span><strong>{money(isPayment ? record.amount.amountMinor : record.total.amountMinor)}</strong></div><Status value={record.status} /></div><dl className="admin-detail-list"><div><dt>Mã đặt vé</dt><dd>{booking?.bookingCode ?? '—'}</dd></div><div><dt>Phim</dt><dd>{booking?.movie.title ?? '—'}</dd></div><div><dt>Rạp chiếu</dt><dd>{booking?.cinema.name ?? '—'}</dd></div><div><dt>Suất chiếu</dt><dd>{booking ? dateTime(booking.showtime.startsAt) : '—'}</dd></div><div><dt>Ghế ngồi</dt><dd>{booking?.items.map(item => item.seatLabel).join(', ') ?? '—'}</dd></div><div><dt>Thời điểm tạo</dt><dd>{dateTime(record.createdAt)}</dd></div>{isPayment && <div><dt>Phương thức</dt><dd>{record.method}</dd></div>}</dl><div className="admin-dialog-actions"><button className="admin-button primary" onClick={onClose}>Đóng chi tiết</button></div></div></Modal>
}
