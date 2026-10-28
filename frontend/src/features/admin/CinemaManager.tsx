import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAdminAuth } from '../auth/AdminAuthContext'
import { cinemaAdminApi, cinemaErrorMessage, type AdminCinema, type CinemaInput } from './cinemaAdminApi'
import { Empty, Icon, Modal, PageHeading } from './AdminUI'
import { CinemaLoadError } from './CinemaLoadError'
import { RoomManager } from './RoomManager'

export function CinemaManager() {
  const token = useAdminAuth().session!.token
  const [search, setSearch] = useState('')
  const [city, setCity] = useState('')
  const [editor, setEditor] = useState<{ record?: AdminCinema } | null>(null)
  const [managedCinema, setManagedCinema] = useState<AdminCinema | null>(null)
  const query = useQuery({ queryKey: ['cinema-admin', 'list'], queryFn: ({ signal }) => cinemaAdminApi.list(token, signal), retry: false })
  const term = search.trim().toLocaleLowerCase('vi-VN')
  const cinemas = query.data?.filter(c => (!city || c.city === city) && `${c.name} ${c.address} ${c.city}`.toLocaleLowerCase('vi-VN').includes(term)) ?? []
  return <>
    <PageHeading title="Rạp chiếu phim" description="Quản lý rạp, phòng chiếu và sơ đồ ghế từ hệ thống."><button className="admin-button primary" onClick={() => setEditor({})}><Icon name="plus" />Thêm rạp</button></PageHeading>
    <section className="admin-panel admin-cinema-directory">
      <div className="admin-record-toolbar"><h2>Danh sách rạp</h2><div className="admin-filters"><label className="admin-search"><Icon name="search" /><input aria-label="Tìm kiếm danh sách" type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Tìm kiếm…" /></label><select aria-label="Lọc danh sách" value={city} onChange={e => setCity(e.target.value)}><option value="">Tất cả thành phố</option>{[...new Set(query.data?.map(c => c.city))].map(value => <option key={value}>{value}</option>)}</select></div></div>
      {query.isPending ? <p className="admin-empty" role="status">Đang tải rạp…</p> : query.isError ? <CinemaLoadError error={query.error} retry={() => void query.refetch()} /> : !cinemas.length ? <Empty>Chưa có rạp phù hợp. Thêm rạp hoặc thay đổi bộ lọc.</Empty> : <div className="admin-cinema-grid">{cinemas.map(cinema => <article className="admin-cinema-card" key={cinema.id}>
        <div className="admin-cinema-card-top"><span className="admin-cinema-icon"><Icon name="cinemas" /></span><span className="admin-cinema-city">{cinema.city}</span></div>
        <div className="admin-cinema-card-heading"><h3>{cinema.name}</h3><p>{cinema.address}</p><p>Múi giờ: {cinema.timezone}</p></div>
        <div className="admin-cinema-card-bottom"><div className="admin-room-actions"><button className="admin-button" aria-label={`Chỉnh sửa ${cinema.name}`} onClick={() => setEditor({ record: cinema })}><Icon name="edit" />Chỉnh sửa</button><button className="admin-button primary" aria-label={`Quản lý phòng ${cinema.name}`} onClick={() => setManagedCinema(cinema)}><Icon name="cinemas" />Quản lý phòng</button></div><Link to={`/admin/showtimes?cinemaId=${encodeURIComponent(cinema.id)}`}>Xem lịch chiếu <Icon name="arrow" /></Link></div>
      </article>)}</div>}
    </section>
    {editor && <CinemaEditor record={editor.record} onClose={() => setEditor(null)} />}
    {managedCinema && <RoomManager cinema={managedCinema} onClose={() => setManagedCinema(null)} />}
  </>
}

function CinemaEditor({ record, onClose }: { record?: AdminCinema; onClose: () => void }) {
  const token = useAdminAuth().session!.token
  const client = useQueryClient()
  const save = useMutation({ mutationFn: (input: CinemaInput) => cinemaAdminApi.saveCinema(token, input, record?.id), retry: false, onSuccess: async () => {
    await client.invalidateQueries({ queryKey: ['cinema-admin'] })
    await client.invalidateQueries({ queryKey: ['cinemas'] })
    onClose()
  } })
  const close = () => { if (!save.isPending) onClose() }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (save.isPending) return
    const fields = new FormData(event.currentTarget)
    const text = (name: string) => String(fields.get(name) ?? '').trim()
    save.mutate({ name: text('name'), address: text('address'), city: text('city'), timezone: text('timezone') })
  }
  return <Modal title={`${record ? 'Chỉnh sửa' : 'Thêm'} rạp chiếu phim`} onClose={close}><form className="admin-form" onSubmit={submit}>
    <fieldset className="admin-form" disabled={save.isPending}>
      <label>Tên rạp<input name="name" required autoFocus maxLength={160} defaultValue={record?.name} /></label>
      <label>Thành phố<input name="city" required maxLength={100} defaultValue={record?.city} /></label>
      <label>Địa chỉ<input name="address" required maxLength={300} defaultValue={record?.address} /></label>
      <label>Múi giờ<input name="timezone" required maxLength={80} defaultValue={record?.timezone ?? 'Asia/Ho_Chi_Minh'} /></label>
    </fieldset>
    {save.isError && <p className="admin-form-error" role="alert">{cinemaErrorMessage(save.error)}</p>}
    <div className="admin-dialog-actions"><button className="admin-button" type="button" disabled={save.isPending} onClick={close}>Hủy</button><button className="admin-button primary" disabled={save.isPending}>{save.isPending ? 'Đang lưu…' : 'Lưu thay đổi'}</button></div>
  </form></Modal>
}
