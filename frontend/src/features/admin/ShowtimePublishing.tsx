import { useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { movieService } from '../movies/api'
import { useAdminAuth } from '../auth/AdminAuthContext'
import { cinemaAdminApi, cinemaErrorMessage, type ShowtimeDto } from './cinemaAdminApi'
import { dateTime, Empty, money, PageHeading, Status } from './AdminUI'

const today = () => new Date().toISOString().slice(0, 10)
const toIso = (local: string) => new Date(local).toISOString()
const CANCEL_REASONS = [['MOVIE_PULLED', 'Gỡ phim'], ['TECHNICAL_ISSUE', 'Sự cố kỹ thuật'], ['LOW_DEMAND', 'Ít khách'], ['OTHER', 'Khác']] as const

export function ShowtimePublishing() {
  const { session } = useAdminAuth()
  const token = session!.token
  const [cinemaId, setCinemaId] = useState('')
  const [date, setDate] = useState(today())
  const [rows, setRows] = useState<Record<string, ShowtimeDto>>({})
  const [notice, setNotice] = useState('')
  const cinemas = useQuery({ queryKey: ['cinema-admin', 'list'], queryFn: cinemaAdminApi.cinemas })
  const activeCinema = cinemaId || cinemas.data?.[0]?.id || ''
  const detail = useQuery({ queryKey: ['cinema-admin', activeCinema], queryFn: () => cinemaAdminApi.cinema(activeCinema), enabled: !!activeCinema })
  const movies = useQuery({ queryKey: ['cinema-admin', 'movies'], queryFn: ({ signal }) => movieService.list({}, signal) })
  const published = useQuery({ queryKey: ['cinema-admin', activeCinema, 'showtimes', date], queryFn: () => cinemaAdminApi.published(activeCinema, date), enabled: !!activeCinema })

  const remember = (s: ShowtimeDto) => { setRows(c => ({ ...c, [s.id]: s })); void published.refetch() }
  const create = useMutation({ mutationFn: (input: Parameters<typeof cinemaAdminApi.create>[1]) => cinemaAdminApi.create(token, input), onSuccess: s => { remember(s); setNotice('Đã tạo suất DRAFT. Chưa có event nào được ghi, khách chưa thấy.') } })
  const publish = useMutation({ mutationFn: (id: string) => cinemaAdminApi.publish(token, id), onSuccess: s => { remember(s); setNotice('Đã PUBLISH. Khách thấy suất ngay. Event ShowtimePublished đã vào outbox, relay đẩy lên Kafka sau ~1 giây.') } })
  const cancel = useMutation({ mutationFn: (v: { id: string; reason: string }) => cinemaAdminApi.cancel(token, v.id, v.reason), onSuccess: s => { remember(s); setNotice('Đã hủy. Khách không còn thấy suất. Event ShowtimeCancelled đã vào outbox (nếu suất từng PUBLISH).') } })
  const error = create.error ?? publish.error ?? cancel.error
  const busy = create.isPending || publish.isPending || cancel.isPending

  const merged = new Map<string, ShowtimeDto>()
  for (const s of Object.values(rows)) if (s.cinemaId === activeCinema) merged.set(s.id, s)
  for (const s of published.data ?? []) merged.set(s.id, s)
  const list = [...merged.values()].sort((a, b) => Date.parse(a.startsAt) - Date.parse(b.startsAt))
  const movieName = (id: string) => movies.data?.find(m => m.id === id)?.title ?? id.slice(0, 8)
  const roomName = (id: string) => detail.data?.auditoriums.find(a => a.id === id)?.name ?? id.slice(0, 8)

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const f = new FormData(event.currentTarget)
    const text = (k: string) => String(f.get(k) ?? '')
    const startsAt = toIso(text('startsAt'))
    create.mutate({ auditoriumId: text('auditoriumId'), movieId: text('movieId'), startsAt, endsAt: toIso(text('endsAt')),
      salesCloseAt: text('salesCloseAt') ? toIso(text('salesCloseAt')) : startsAt, priceMinor: Number(text('price')), currency: 'VND' })
  }

  return <>
    <PageHeading title="Phát hành suất chiếu" description="Tạo, phát hành và hủy suất chiếu qua cinema-service. Phát hành/hủy ghi một event vào outbox." />
    <section className="admin-panel" style={{ marginBottom: 20 }}>
      <div className="admin-panel-heading"><div><h2>Tạo suất chiếu (DRAFT)</h2></div></div>
      <form className="admin-form" style={{ padding: '0 24px 24px' }} onSubmit={submit}>
        <div className="admin-form-row">
          <label>Rạp<select value={activeCinema} onChange={e => setCinemaId(e.target.value)}>{cinemas.data?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
          <label>Phòng chiếu<select name="auditoriumId" required key={activeCinema}>{detail.data?.auditoriums.map(a => <option key={a.id} value={a.id}>{a.name} ({a.seatCount} ghế)</option>)}</select></label>
        </div>
        <label>Phim<select name="movieId" required>{movies.data?.map(m => <option key={m.id} value={m.id}>{m.title}</option>)}</select></label>
        <div className="admin-form-row">
          <label>Bắt đầu<input name="startsAt" type="datetime-local" required /></label>
          <label>Kết thúc<input name="endsAt" type="datetime-local" required /></label>
          <label>Đóng bán vé (mặc định = giờ bắt đầu)<input name="salesCloseAt" type="datetime-local" /></label>
        </div>
        <label>Giá vé (VND)<input name="price" type="number" min="0" step="1000" required defaultValue={95000} /></label>
        {error && <p className="admin-form-error" role="alert">{cinemaErrorMessage(error)}</p>}
        <div className="admin-dialog-actions"><button className="admin-button primary" type="submit" disabled={busy || !detail.data}>{create.isPending ? 'Đang tạo…' : 'Tạo suất DRAFT'}</button></div>
      </form>
    </section>
    <section className="admin-panel">
      <div className="admin-panel-heading"><div><h2>Suất chiếu</h2><p>Server chỉ liệt kê suất PUBLISHED theo ngày; DRAFT/CANCELLED hiện khi bạn vừa thao tác.</p></div>
        <input aria-label="Ngày" type="date" value={date} onChange={e => setDate(e.target.value)} /></div>
      {notice && <p className="admin-form-note" role="status" style={{ padding: '0 24px' }}>{notice}</p>}
      {list.length === 0 ? <Empty>Chưa có suất chiếu cho rạp và ngày này.</Empty> :
        <div className="admin-table-scroll"><table className="admin-table"><thead><tr><th>Phim</th><th>Phòng</th><th>Bắt đầu</th><th>Giá vé</th><th>Trạng thái</th><th><span className="admin-sr-only">Thao tác</span></th></tr></thead>
          <tbody>{list.map(s => <tr key={s.id}>
            <td>{movieName(s.movieId)}</td><td>{roomName(s.auditoriumId)}</td><td>{dateTime(s.startsAt)}</td><td>{money(s.price.amountMinor)}</td><td><Status value={s.status} /></td>
            <td><div className="admin-room-actions">
              {s.status === 'DRAFT' && <button className="admin-button primary" disabled={busy} onClick={() => publish.mutate(s.id)}>Phát hành</button>}
              {s.status !== 'CANCELLED' && <select aria-label="Lý do hủy" defaultValue="" disabled={busy} onChange={e => { if (e.target.value) cancel.mutate({ id: s.id, reason: e.target.value }) }}>
                <option value="">Hủy suất…</option>{CANCEL_REASONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select>}
            </div></td></tr>)}</tbody></table></div>}
    </section>
  </>
}