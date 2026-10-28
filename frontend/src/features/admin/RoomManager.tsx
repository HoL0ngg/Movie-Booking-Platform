import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { Auditorium, Cinema, SeatType } from '../../shared/contracts'
import { changeSeatType, type PhysicalSeat } from './seatLayout'
import { cinemaAdminApi, cinemaErrorMessage } from './cinemaAdminApi'
import { useAdminAuth } from '../auth/AdminAuthContext'
import { CinemaLoadError } from './CinemaLoadError'
import { Icon, Modal } from './AdminUI'

const seatTypes: Record<SeatType, string> = { STANDARD: 'Thường', VIP: 'VIP', COUPLE: 'Đôi' }

export function RoomManager({ cinema, onClose }: { cinema: Cinema; onClose: () => void }) {
  const queryClient = useQueryClient()
  const token = useAdminAuth().session!.token
  const [editingRoom, setEditingRoom] = useState<Auditorium | null>(null)
  const [layoutRoom, setLayoutRoom] = useState<Auditorium | null>(null)
  const [name, setName] = useState('')
  const save = useMutation({ mutationFn: (room: { id?: string; name: string }) => cinemaAdminApi.saveRoom(token, cinema.id, room.name, room.id), retry: false, onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['cinema-admin', cinema.id, 'rooms'] })
    setName('')
    setEditingRoom(null)
  } })
  const roomsQuery = useQuery({ queryKey: ['cinema-admin', cinema.id, 'rooms'], queryFn: ({ signal }) => cinemaAdminApi.rooms(token, cinema.id, signal), retry: false })
  const rooms = roomsQuery.data ?? []
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!save.isPending) save.mutate({ id: editingRoom?.id, name: name.trim() })
  }
  if (layoutRoom) return <SeatLayoutEditor key={layoutRoom.id} room={layoutRoom} onClose={() => setLayoutRoom(null)} />
  return <Modal title={`Quản lý phòng · ${cinema.name}`} onClose={() => { if (!save.isPending) onClose() }}>
    <div className="admin-room-manager">
      <form className="admin-form admin-room-create" onSubmit={submit}>
        <label>{editingRoom ? 'Đổi tên phòng' : 'Tên phòng mới'}<input required maxLength={100} value={name} onChange={event => setName(event.target.value)} placeholder="Ví dụ: Phòng 02" disabled={save.isPending} /></label>
        <div className="admin-room-actions"><button className="admin-button primary" disabled={save.isPending}><Icon name={editingRoom ? 'edit' : 'plus'} />{save.isPending ? 'Đang lưu…' : editingRoom ? 'Lưu tên phòng' : 'Thêm phòng'}</button>{editingRoom && <button type="button" className="admin-button" disabled={save.isPending} onClick={() => { setEditingRoom(null); setName(''); save.reset() }}>Hủy đổi tên</button>}</div>
      </form>
      {save.isError && <p className="admin-form-error" role="alert">{cinemaErrorMessage(save.error)}</p>}
      {roomsQuery.isPending ? <p role="status">Đang tải phòng…</p> : roomsQuery.isError ? <CinemaLoadError error={roomsQuery.error} retry={() => void roomsQuery.refetch()} /> : <div className="admin-room-list">{rooms.length ? rooms.map(room => {
        return <article className="admin-room-card" key={room.id}>
          <div><h3>{room.name}</h3></div>
          <div className="admin-room-actions"><button className="admin-icon-button" disabled={save.isPending} aria-label={`Đổi tên ${room.name}`} onClick={() => { setEditingRoom(room); setName(room.name); save.reset() }}><Icon name="edit" /></button><button className="admin-button" disabled={save.isPending} aria-label={`Chỉnh sơ đồ ghế ${room.name}`} onClick={() => setLayoutRoom(room)}><Icon name="overview" />Chỉnh sơ đồ ghế</button></div>
        </article>
      }) : <p className="admin-form-note">Rạp chưa có phòng chiếu.</p>}</div>}
      <div className="admin-dialog-actions"><button className="admin-button" disabled={save.isPending} onClick={onClose}>Đóng</button></div>
    </div>
  </Modal>
}

function SeatLayoutEditor({ room, onClose }: { room: Auditorium; onClose: () => void }) {
  const token = useAdminAuth().session!.token
  const seats = useQuery({ queryKey: ['cinema-admin', room.id, 'seats'], queryFn: ({ signal }) => cinemaAdminApi.seats(token, room.id, signal), retry: false })
  const showtimes = useQuery({ queryKey: ['cinema-admin', room.cinemaId, 'showtimes'], queryFn: ({ signal }) => cinemaAdminApi.showtimes(token, room.cinemaId, signal), retry: false })
  const error = seats.error ?? showtimes.error
  if (error) return <Modal title={`Sơ đồ ghế · ${room.name}`} onClose={onClose}><CinemaLoadError error={error} retry={() => { void seats.refetch(); void showtimes.refetch() }} /></Modal>
  if (!seats.isSuccess || !showtimes.isSuccess) return <Modal title={`Sơ đồ ghế · ${room.name}`} onClose={onClose}><p role="status">Đang tải sơ đồ ghế…</p></Modal>
  return <SeatLayoutForm room={room} original={seats.data} locked={showtimes.data.some(s => s.auditoriumId === room.id)} onClose={onClose} />
}

function SeatLayoutForm({ room, original, locked, onClose }: { room: Auditorium; original: PhysicalSeat[]; locked: boolean; onClose: () => void }) {
  const queryClient = useQueryClient()
  const token = useAdminAuth().session!.token
  const [seats, setSeats] = useState<PhysicalSeat[]>(original)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [selectionError, setSelectionError] = useState('')
  const [rowCount, setRowCount] = useState(new Set(original.map(seat => seat.rowLabel)).size || 6)
  const [columnCount, setColumnCount] = useState(original.length ? Math.max(...original.map(seat => seat.seatNumber)) : 10)
  const selectedSeats = seats.filter(seat => selectedIds.includes(seat.id))
  const selected = selectedSeats.length === 1 ? selectedSeats[0] : undefined
  const rows = [...new Set(seats.map(seat => seat.rowLabel))].sort()
  const columns = Math.min(30, Math.max(1, ...seats.map(seat => seat.seatNumber)))
  const dirty = JSON.stringify(seats) !== JSON.stringify(original)
  const save = useMutation({ mutationFn: (input: { auditoriumId: string; seats: PhysicalSeat[] }) => cinemaAdminApi.saveSeats(token, input), retry: false, onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['cinema-admin', room.id, 'seats'] })
    onClose()
  } })
  function close() {
    if (!save.isPending && (!dirty || window.confirm('Bỏ các thay đổi sơ đồ chưa lưu?'))) onClose()
  }
  function generate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (locked || save.isPending || !Number.isInteger(rowCount) || rowCount < 1 || rowCount > 26 || !Number.isInteger(columnCount) || columnCount < 1 || columnCount > 30) return
    if (seats.length && !window.confirm('Tạo lại sơ đồ theo kích thước mới? Các vị trí ngoài kích thước này sẽ bị bỏ khỏi bản chỉnh sửa.')) return
    const nextSeats: PhysicalSeat[] = Array.from({ length: rowCount }, (_, index) => String.fromCharCode(65 + index)).flatMap(rowLabel => Array.from({ length: columnCount }, (_, index) => {
      const seatNumber = index + 1
      return seats.find(seat => seat.rowLabel === rowLabel && seat.seatNumber === seatNumber) ?? { id: crypto.randomUUID(), auditoriumId: room.id, rowLabel, seatNumber, seatType: 'STANDARD' as const, isActive: true }
    }))
    if (nextSeats.some(seat => seat.pairedSeatId && !nextSeats.some(item => item.id === seat.pairedSeatId))) {
      setSelectionError('Kích thước mới sẽ tách một cặp ghế đôi. Đổi cặp đó sang ghế thường trước khi tạo lại sơ đồ.')
      return
    }
    setSeats(nextSeats)
    setSelectedIds([])
    setSelectionError('')
    save.reset()
  }
  function updateSelected(changes: Partial<PhysicalSeat>) {
    if (!locked && !save.isPending) setSeats(items => items.map(seat => seat.id === selected?.id ? { ...seat, ...changes } : seat))
    save.reset()
  }
  function toggleSeat(seat: PhysicalSeat) {
    const ids = [seat.id, ...(seat.pairedSeatId && seats.some(item => item.id === seat.pairedSeatId) ? [seat.pairedSeatId] : [])]
    toggleSeats(ids)
  }
  function toggleSeats(ids: string[]) {
    if (locked || save.isPending) return
    setSelectedIds(current => ids.every(id => current.includes(id)) ? current.filter(id => !ids.includes(id)) : [...new Set([...current, ...ids])])
    setSelectionError('')
  }
  function applyType(type: SeatType) {
    if (locked || save.isPending) return
    try { setSeats(changeSeatType(seats, selectedIds, type)); setSelectedIds([]); setSelectionError(''); save.reset() }
    catch (error) { setSelectionError(error instanceof Error ? error.message : 'Không thể đổi loại ghế.') }
  }
  function setSelectedActive(isActive: boolean) {
    if (locked || save.isPending || !selectedIds.length) return
    setSeats(items => items.map(seat => selectedIds.includes(seat.id) ? { ...seat, isActive } : seat))
    setSelectionError('')
    save.reset()
  }
  return <Modal title={`Chỉnh sơ đồ ghế · ${room.name}`} onClose={close}>
    <div className="admin-seat-editor">
      {locked && <p className="admin-form-note" role="status">Chỉ xem · Phòng đã có lịch chiếu</p>}
      <form className="admin-form admin-seat-dimensions" onSubmit={generate}>
        <label>Số hàng<input type="number" min={1} max={26} required value={Number.isNaN(rowCount) ? '' : rowCount} onChange={event => setRowCount(event.target.valueAsNumber)} disabled={locked || save.isPending} /></label>
        <label>Ghế mỗi hàng<input type="number" min={1} max={30} required value={Number.isNaN(columnCount) ? '' : columnCount} onChange={event => setColumnCount(event.target.valueAsNumber)} disabled={locked || save.isPending} /></label>
        <button className="admin-button" disabled={locked || save.isPending}>{seats.length ? 'Tạo lại sơ đồ' : 'Tạo sơ đồ'}</button>
      </form>
      <div className="admin-seat-preview"><div className="admin-seat-screen">MÀN HÌNH</div><div className="admin-seat-scroll" role="group" aria-label={`Sơ đồ ghế ${room.name}`}>
        {rows.length ? rows.map(row => <div className="admin-seat-row" key={row}><button type="button" className="admin-seat-row-toggle" aria-label={`Chọn cả hàng ${row}`} aria-pressed={seats.filter(seat => seat.rowLabel === row).every(seat => selectedIds.includes(seat.id))} title={`Chọn hoặc bỏ chọn cả hàng ${row}`} disabled={locked || save.isPending} onClick={() => toggleSeats(seats.filter(seat => seat.rowLabel === row).map(seat => seat.id))}>{row || '?'}</button><div style={{ gridTemplateColumns: `repeat(${columns}, 36px)` }}>{Array.from({ length: columns }, (_, index) => {
          const seat = seats.find(item => item.rowLabel === row && item.seatNumber === index + 1)
          const partner = seats.find(item => item.id === seat?.pairedSeatId)
          return seat ? <button type="button" key={index} className={`admin-physical-seat ${seat.seatType.toLowerCase()} ${seat.isActive ? '' : 'inactive'} ${partner && seat.seatNumber < partner.seatNumber ? 'pair-start' : ''}`} title={`${seat.rowLabel}${seat.seatNumber} · ${seatTypes[seat.seatType]}`} aria-label={`${seat.rowLabel}${seat.seatNumber}, ghế ${seatTypes[seat.seatType]}${partner ? `, ghép với ${partner.rowLabel}${partner.seatNumber}` : ''}, ${seat.isActive ? 'sử dụng' : 'tạm khóa'}`} aria-pressed={selectedIds.includes(seat.id)} disabled={locked || save.isPending} onClick={() => toggleSeat(seat)}>{seat.isActive ? <>{seat.seatNumber}{seat.seatType === 'COUPLE' && <small>Đ</small>}</> : '×'}</button> : <span className="admin-seat-gap" key={index} />
        })}</div></div>) : <p className="admin-form-note">Chưa có sơ đồ ghế.</p>}
      </div></div>
      <div className="admin-seat-legend"><span>Thường</span><span>VIP · Vàng</span><span>Đ · Đôi (2 ghế)</span><span>× Tạm khóa</span></div>
      {!!seats.length && <div className="admin-seat-batch"><p role="status">{selectedSeats.length ? `Đã chọn ${selectedSeats.length} ghế: ${selectedSeats.map(seat => `${seat.rowLabel}${seat.seatNumber}`).join(', ')}` : 'Chưa chọn ghế'}</p><div className="admin-room-actions"><button className="admin-button" disabled={locked || save.isPending || !selectedSeats.length} onClick={() => applyType('STANDARD')}>Đổi sang thường</button><button className="admin-button" disabled={locked || save.isPending || !selectedSeats.length} onClick={() => applyType('VIP')}>Đổi sang VIP</button><button className="admin-button" disabled={locked || save.isPending || !selectedSeats.length} onClick={() => applyType('COUPLE')}>Ghép ghế đôi</button><button className="admin-button" disabled={locked || save.isPending || !selectedSeats.some(seat => seat.isActive)} onClick={() => setSelectedActive(false)}>Khóa tạm thời</button><button className="admin-button" disabled={locked || save.isPending || !selectedSeats.some(seat => !seat.isActive)} onClick={() => setSelectedActive(true)}>Mở khóa</button><button className="admin-button" disabled={save.isPending || !selectedSeats.length} onClick={() => { setSelectedIds([]); setSelectionError('') }}>Bỏ chọn</button></div></div>}
      {selectionError && <p className="admin-form-error" role="alert">{selectionError}</p>}
      {selected && <fieldset className="admin-form admin-seat-selection" disabled={locked || save.isPending}><legend>Ghế {selected.rowLabel}{selected.seatNumber}</legend><div className="admin-form-row"><label>Hàng ghế<input maxLength={1} pattern="[A-Za-z]" required value={selected.rowLabel} onChange={event => updateSelected({ rowLabel: event.target.value.toUpperCase() })} /></label><label>Số ghế<input type="number" min={1} max={30} required value={Number.isNaN(selected.seatNumber) ? '' : selected.seatNumber} onChange={event => updateSelected({ seatNumber: event.target.valueAsNumber })} /></label></div></fieldset>}
      {save.isError && <p className="admin-form-error" role="alert">{cinemaErrorMessage(save.error)}</p>}
      <div className="admin-dialog-actions"><button className="admin-button" disabled={save.isPending} onClick={close}>Quay lại</button><button className="admin-button primary" disabled={locked || !dirty || !seats.length || save.isPending} onClick={() => save.mutate({ auditoriumId: room.id, seats })}>{save.isPending ? 'Đang lưu…' : 'Lưu sơ đồ'}</button></div>
    </div>
  </Modal>
}
