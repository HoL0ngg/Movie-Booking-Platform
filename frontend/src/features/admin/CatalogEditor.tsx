import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { Cinema, Movie, Showtime } from '../../types/contracts'
import { adminService, type AdminData, type CatalogKind, type CatalogRecord } from './adminService'
import { Modal } from './AdminUI'

const localDateTime = (value: string) => { const date = new Date(value); date.setMinutes(date.getMinutes() - date.getTimezoneOffset()); return date.toISOString().slice(0, 16) }
export function CatalogEditor({ kind, record, data, onClose, onSaved }: { kind: CatalogKind; record?: CatalogRecord; data: AdminData; onClose: () => void; onSaved: () => void }) {
  const queryClient = useQueryClient()
  const movie = kind === 'movies' ? record as Movie | undefined : undefined
  const cinema = kind === 'cinemas' ? record as Cinema | undefined : undefined
  const slot = kind === 'showtimes' ? record as Showtime | undefined : undefined
  const [cinemaId, setCinemaId] = useState(slot?.cinemaId ?? data.cinemas[0]?.id ?? '')
  const save = useMutation({ mutationFn: adminService.save, onSuccess: result => { queryClient.setQueryData(['admin-demo'], result); onSaved(); onClose() } })
  const names = { movies: 'phim', cinemas: 'rạp chiếu phim', showtimes: 'suất chiếu' }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const fields = new FormData(event.currentTarget)
    const text = (name: string) => String(fields.get(name) ?? '').trim()
    const id = record?.id ?? crypto.randomUUID()
    let updated: CatalogRecord
    if (kind === 'movies') {
      updated = { id, title: text('title'), originalTitle: text('originalTitle'), synopsis: text('synopsis'), genres: text('genres').split(',').map(value => value.trim()).filter(Boolean), director: text('director'), cast: text('cast').split(',').map(value => value.trim()).filter(Boolean), durationMinutes: Number(text('durationMinutes')), ageRating: text('ageRating'), releaseDate: text('releaseDate'), status: text('status') as Movie['status'], posterUrl: movie?.posterUrl ?? '', backdropUrl: movie?.backdropUrl ?? '', rating: movie?.rating ?? 0 }
    } else if (kind === 'cinemas') {
      updated = { id, name: text('name'), address: text('address'), city: text('city') }
    } else {
      updated = { id, movieId: text('movieId'), cinemaId, auditoriumId: text('auditoriumId'), startsAt: new Date(text('startsAt')).toISOString(), endsAt: new Date(text('endsAt')).toISOString(), price: { amountMinor: Number(text('price')), currency: 'VND' }, format: text('format') as Showtime['format'], language: text('language') }
    }
    save.mutate({ kind, record: updated })
  }
  return <Modal title={`${record ? 'Chỉnh sửa' : 'Thêm'} ${names[kind]}`} onClose={onClose}>
    <form className="admin-form" onSubmit={submit}>
      <p className="admin-form-note">{record ? 'Cập nhật thông tin trong không gian demo.' : 'Tạo mới dữ liệu trong không gian demo.'}</p>
      {kind === 'movies' && <>
        <label>Tên phim<input name="title" required maxLength={160} defaultValue={movie?.title} autoFocus /></label>
        <label>Tên gốc<input name="originalTitle" maxLength={160} defaultValue={movie?.originalTitle} /></label>
        <div className="admin-form-row"><label>Thời lượng (phút)<input name="durationMinutes" type="number" required min="1" max="600" step="1" defaultValue={movie?.durationMinutes ?? 120} /></label><label>Phân loại độ tuổi<select name="ageRating" defaultValue={movie?.ageRating ?? 'T13'}>{['P', 'K', 'T13', 'T16', 'T18'].map(value => <option key={value}>{value}</option>)}</select></label></div>
        <div className="admin-form-row"><label>Ngày khởi chiếu<input name="releaseDate" type="date" required defaultValue={movie?.releaseDate ?? new Date().toISOString().slice(0, 10)} /></label><label>Trạng thái<select name="status" defaultValue={movie?.status ?? 'COMING_SOON'}><option value="NOW_SHOWING">Đang chiếu</option><option value="COMING_SOON">Sắp chiếu</option></select></label></div>
        <div className="admin-form-row"><label>Đạo diễn<input name="director" maxLength={160} defaultValue={movie?.director} /></label><label>Thể loại (cách nhau bằng dấu phẩy)<input name="genres" maxLength={200} defaultValue={movie?.genres.join(', ')} placeholder="Hành động, Phiêu lưu" /></label></div>
        <label>Diễn viên<input name="cast" maxLength={400} defaultValue={movie?.cast.join(', ')} /></label>
        <label>Tóm tắt nội dung<textarea name="synopsis" required rows={3} maxLength={3000} defaultValue={movie?.synopsis} /></label>
      </>}
      {kind === 'cinemas' && <>
        <label>Tên rạp<input name="name" required autoFocus maxLength={160} defaultValue={cinema?.name} placeholder="Cinémat Central" /></label>
        <label>Thành phố<input name="city" required maxLength={100} defaultValue={cinema?.city} list="admin-cities" /><datalist id="admin-cities"><option>TP. Hồ Chí Minh</option><option>Hà Nội</option><option>Đà Nẵng</option></datalist></label>
        <label>Địa chỉ<input name="address" required maxLength={300} defaultValue={cinema?.address} placeholder="Số nhà, đường, quận/huyện" /></label>
        {!record && <p className="admin-form-note">Rạp demo mới được tạo cùng một phòng chiếu mặc định “Phòng 01”.</p>}
      </>}
      {kind === 'showtimes' && <>
        <label>Phim<select name="movieId" required defaultValue={slot?.movieId ?? data.movies[0]?.id}>{data.movies.map(item => <option key={item.id} value={item.id}>{item.title}</option>)}</select></label>
        <div className="admin-form-row"><label>Rạp chiếu<select value={cinemaId} onChange={event => setCinemaId(event.target.value)} required>{data.cinemas.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label><label>Phòng chiếu<select name="auditoriumId" key={cinemaId} required defaultValue={slot?.cinemaId === cinemaId ? slot.auditoriumId : undefined}>{data.auditoriums.filter(item => item.cinemaId === cinemaId).map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label></div>
        <div className="admin-form-row"><label>Bắt đầu<input name="startsAt" type="datetime-local" required defaultValue={slot ? localDateTime(slot.startsAt) : undefined} /></label><label>Kết thúc<input name="endsAt" type="datetime-local" required defaultValue={slot ? localDateTime(slot.endsAt) : undefined} /></label></div>
        <div className="admin-form-row"><label>Giá vé cơ bản (VND)<input name="price" type="number" min="1" max="10000000" step="1" required defaultValue={slot?.price.amountMinor ?? 95000} /></label><label>Định dạng<select name="format" defaultValue={slot?.format ?? '2D'}><option>2D</option><option>IMAX</option></select></label></div>
        <label>Ngôn ngữ<input name="language" required maxLength={100} defaultValue={slot?.language ?? 'Phụ đề Việt'} /></label>
      </>}
      {save.isError && <p className="admin-form-error" role="alert">{save.error.message}</p>}
      <div className="admin-dialog-actions"><button type="button" className="admin-button" onClick={onClose}>Hủy</button><button className="admin-button primary" type="submit" disabled={save.isPending}>{save.isPending ? 'Đang lưu…' : 'Lưu thay đổi'}</button></div>
    </form>
  </Modal>
}
