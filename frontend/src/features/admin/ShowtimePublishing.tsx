import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { movieAdminApi } from './movieAdminApi'
import { useAdminAuth } from '../auth/AdminAuthContext'
import { cinemaAdminApi, cinemaErrorMessage, type ShowtimeDto, type CreateShowtimeInput } from './cinemaAdminApi'
import { CinemaLoadError } from './CinemaLoadError'
import { dateTime, Empty, Modal, money, PageHeading, Status } from './AdminUI'

const localDateTime = (value: string) => { const date = new Date(value); date.setMinutes(date.getMinutes() - date.getTimezoneOffset()); return date.toISOString().slice(0, 16) }
const CANCEL_REASONS = [['MOVIE_PULLED', 'Gỡ phim'], ['TECHNICAL_ISSUE', 'Sự cố kỹ thuật'], ['LOW_DEMAND', 'Ít khách'], ['OTHER', 'Khác']] as const

export function ShowtimePublishing() {
  const token = useAdminAuth().session!.token
  const client = useQueryClient()
  const [params, setParams] = useSearchParams()
  const [date, setDate] = useState('')
  const [editor, setEditor] = useState<{ record?: ShowtimeDto } | null>(null)
  const [notice, setNotice] = useState('')
  const cinemas = useQuery({ queryKey: ['cinema-admin', 'list'], queryFn: ({ signal }) => cinemaAdminApi.list(token, signal), retry: false })
  const cinemaId = params.get('cinemaId') ?? params.get('status') ?? cinemas.data?.[0]?.id ?? ''
  const cinema = cinemas.data?.find(c => c.id === cinemaId)
  const rooms = useQuery({ queryKey: ['cinema-admin', cinemaId, 'rooms'], queryFn: ({ signal }) => cinemaAdminApi.rooms(token, cinemaId, signal), enabled: !!cinema, retry: false })
  const movies = useQuery({ queryKey: ['admin-movies', 'list'], queryFn: () => movieAdminApi.list(token), retry: false })
  const showtimes = useQuery({ queryKey: ['cinema-admin', cinemaId, 'showtimes'], queryFn: ({ signal }) => cinemaAdminApi.showtimes(token, cinemaId, signal), enabled: !!cinema, retry: false })
  const refresh = async () => {
    await client.invalidateQueries({ queryKey: ['cinema-admin', cinemaId, 'showtimes'] })
    await client.invalidateQueries({ queryKey: ['showtimes'] })
    await client.invalidateQueries({ queryKey: ['showtime'] })
  }
  const publish = useMutation({ mutationFn: (id: string) => cinemaAdminApi.publish(token, id), retry: false, onSuccess: async () => { await refresh(); setNotice('Đã phát hành suất chiếu.') } })
  const cancel = useMutation({ mutationFn: (v: { id: string; reason: string }) => cinemaAdminApi.cancel(token, v.id, v.reason), retry: false, onSuccess: async () => { await refresh(); setNotice('Đã hủy suất chiếu.') } })
  const busy = publish.isPending || cancel.isPending
  const error = cinemas.error ?? rooms.error ?? movies.error ?? showtimes.error
  const loading = cinemas.isPending || (!!cinema && (rooms.isPending || showtimes.isPending)) || movies.isPending
  const zone = cinema?.timezone ?? 'Asia/Ho_Chi_Minh'
  const list = (showtimes.data ?? []).filter(s => !date || new Intl.DateTimeFormat('en-CA', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(s.startsAt)) === date).sort((a, b) => Date.parse(a.startsAt) - Date.parse(b.startsAt))
  const movieName = (id: string) => movies.data?.find(m => m.id === id)?.title ?? id
  const roomName = (id: string) => rooms.data?.find(r => r.id === id)?.name ?? id
  return <>
    <PageHeading title="Lịch chiếu" description="Quản lý suất nháp, phát hành và hủy suất chiếu từ hệ thống."><button className="admin-button primary" disabled={busy || !!error || loading || !rooms.data?.length || !movies.data?.some(m => m.status === 'PUBLISHED')} onClick={() => setEditor({})}>Thêm suất chiếu</button></PageHeading>
    <section className="admin-panel">
      <div className="admin-record-toolbar"><div className="admin-filters">
        <label>Rạp<select aria-label="Rạp" value={cinemaId} disabled={busy} onChange={e => { setParams({ cinemaId: e.target.value }); setNotice(''); publish.reset(); cancel.reset() }}><option value="" disabled>Chọn rạp</option>{cinemas.data?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
        <label>Ngày tại rạp<input aria-label="Ngày" type="date" value={date} onChange={e => setDate(e.target.value)} /></label>
        {date && <button className="admin-button" onClick={() => setDate('')}>Tất cả ngày</button>}
      </div></div>
      {notice && <p className="admin-form-note" role="status">{notice}</p>}
      {(publish.error || cancel.error) && <p className="admin-form-error" role="alert">{cinemaErrorMessage(publish.error ?? cancel.error)}</p>}
      {error ? <CinemaLoadError error={error} retry={() => { void cinemas.refetch(); void movies.refetch(); if (cinema) { void rooms.refetch(); void showtimes.refetch() } }} /> : loading ? <p className="admin-empty" role="status">Đang tải lịch chiếu…</p> : !cinema ? <Empty>Chọn một rạp có trong hệ thống để quản lý lịch chiếu.</Empty> : !list.length ? <Empty>Chưa có suất chiếu cho rạp và ngày đã chọn.</Empty> : <div className="admin-table-scroll"><table className="admin-table"><thead><tr><th>Phim</th><th>Phòng</th><th>Bắt đầu</th><th>Kết thúc</th><th>Giá vé</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{list.map(s => <tr key={s.id}>
        <td>{movieName(s.movieId)}</td><td>{roomName(s.auditoriumId)}</td><td>{dateTime(s.startsAt)}</td><td>{dateTime(s.endsAt)}</td><td>{money(s.price.amountMinor)}{s.price.currency !== 'VND' && ` (${s.price.currency})`}</td><td><Status value={s.status} /></td><td><div className="admin-room-actions">
          {s.status === 'DRAFT' && <><button className="admin-button" disabled={busy} aria-label={`Chỉnh sửa suất ${s.id}`} onClick={() => setEditor({ record: s })}>Chỉnh sửa</button><button className="admin-button primary" disabled={busy} onClick={() => { publish.reset(); cancel.reset(); publish.mutate(s.id) }}>Phát hành</button></>}
          {s.status !== 'CANCELLED' && <select key={`${s.id}-${s.status}`} aria-label={`Lý do hủy ${s.id}`} value="" disabled={busy} onChange={e => { if (e.target.value) { publish.reset(); cancel.reset(); cancel.mutate({ id: s.id, reason: e.target.value }) } }}><option value="">Hủy suất…</option>{CANCEL_REASONS.map(([v, label]) => <option key={v} value={v}>{label}</option>)}</select>}
        </div></td>
      </tr>)}</tbody></table></div>}
    </section>
    {editor && <ShowtimeEditor cinemaId={cinemaId} record={editor.record} onClose={() => setEditor(null)} onSaved={async () => { await refresh(); setNotice('Đã lưu suất nháp. Phát hành để khách có thể xem suất chiếu.') }} />}
  </>
}

function ShowtimeEditor({ cinemaId, record, onClose, onSaved }: { cinemaId: string; record?: ShowtimeDto; onClose: () => void; onSaved: () => Promise<void> }) {
  const token = useAdminAuth().session!.token
  const [validation, setValidation] = useState('')
  const rooms = useQuery({ queryKey: ['cinema-admin', cinemaId, 'rooms'], queryFn: ({ signal }) => cinemaAdminApi.rooms(token, cinemaId, signal), retry: false })
  const movies = useQuery({ queryKey: ['admin-movies', 'list'], queryFn: () => movieAdminApi.list(token), retry: false })
  const save = useMutation({ mutationFn: (input: CreateShowtimeInput) => record ? cinemaAdminApi.update(token, record.id, input) : cinemaAdminApi.create(token, input), retry: false, onSuccess: async () => { await onSaved(); onClose() } })
  const close = () => { if (!save.isPending) onClose() }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (save.isPending) return
    const fields = new FormData(event.currentTarget)
    const text = (key: string) => String(fields.get(key) ?? '')
    const start = Date.parse(text('startsAt')), end = Date.parse(text('endsAt')), sales = Date.parse(text('salesCloseAt') || text('startsAt'))
    const priceMinor = Number(text('price'))
    if (![start, end, sales].every(Number.isFinite) || start >= end || sales > start || !Number.isSafeInteger(priceMinor) || priceMinor < 0) { setValidation('Kiểm tra thời gian: bắt đầu < kết thúc, đóng bán ≤ bắt đầu; giá vé phải là số nguyên không âm.'); return }
    setValidation('')
    save.mutate({ auditoriumId: record?.auditoriumId ?? text('auditoriumId'), movieId: text('movieId'), startsAt: new Date(start).toISOString(), endsAt: new Date(end).toISOString(), salesCloseAt: new Date(sales).toISOString(), priceMinor, currency: record?.price.currency ?? 'VND' })
  }
  const error = rooms.error ?? movies.error
  return <Modal title={record ? 'Chỉnh sửa suất nháp' : 'Thêm suất chiếu'} onClose={close}>
    {error ? <CinemaLoadError error={error} retry={() => { void rooms.refetch(); void movies.refetch() }} /> : rooms.isPending || movies.isPending ? <p role="status">Đang tải dữ liệu…</p> : <form className="admin-form" onSubmit={submit}>
      <fieldset className="admin-form" disabled={save.isPending}>
        <label>Phim<select name="movieId" required defaultValue={record?.movieId}>{movies.data?.filter(m => m.status === 'PUBLISHED' || m.id === record?.movieId).map(m => <option key={m.id} value={m.id}>{m.title}</option>)}</select></label>
        <label>Phòng chiếu<select name="auditoriumId" required disabled={!!record} defaultValue={record?.auditoriumId}>{rooms.data?.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}</select></label>
        <p className="admin-form-note">Nhập thời gian theo múi giờ thiết bị ({Intl.DateTimeFormat().resolvedOptions().timeZone}).</p>
        <div className="admin-form-row"><label>Bắt đầu<input name="startsAt" type="datetime-local" required defaultValue={record ? localDateTime(record.startsAt) : ''} /></label><label>Kết thúc<input name="endsAt" type="datetime-local" required defaultValue={record ? localDateTime(record.endsAt) : ''} /></label></div>
        <label>Đóng bán vé<input name="salesCloseAt" type="datetime-local" defaultValue={record ? localDateTime(record.salesCloseAt) : ''} /></label>
        <label>Giá vé ({record?.price.currency ?? 'VND'})<input name="price" type="number" min="0" step="1" required defaultValue={record?.price.amountMinor ?? 95000} /></label>
      </fieldset>
      {(validation || save.error) && <p className="admin-form-error" role="alert">{validation || cinemaErrorMessage(save.error)}</p>}
      <div className="admin-dialog-actions"><button className="admin-button" type="button" disabled={save.isPending} onClick={close}>Hủy</button><button className="admin-button primary" disabled={save.isPending}>{save.isPending ? 'Đang lưu…' : 'Lưu suất nháp'}</button></div>
    </form>}
  </Modal>
}
