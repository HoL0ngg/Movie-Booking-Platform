import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { Auditorium, Cinema, SeatType } from '../../shared/contracts'
import { adminService, changeSeatType, type AdminData, type PhysicalSeat } from './adminService'
import { Icon, Modal } from './AdminUI'

const seatTypes: Record<SeatType, string> = { STANDARD: 'Thường', VIP: 'VIP', COUPLE: 'Đôi' }

export function RoomManager({ cinema, data, onClose }: { cinema: Cinema; data: AdminData; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [editingRoom, setEditingRoom] = useState<Auditorium | null>(null)
  const [layoutRoom, setLayoutRoom] = useState<Auditorium | null>(null)
  const [name, setName] = useState('')
  const save = useMutation({ mutationFn: adminService.saveRoom, onSuccess: result => {
    queryClient.setQueryData(['admin-demo'], result)
    setName('')
    setEditingRoom(null)
  } })
  const rooms = data.auditoriums.filter(room => room.cinemaId === cinema.id)
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    save.mutate({ id: editingRoom?.id ?? crypto.randomUUID(), cinemaId: cinema.id, name })
  }
  if (layoutRoom) return <SeatLayoutEditor key={layoutRoom.id} room={layoutRoom} data={data} onClose={() => setLayoutRoom(null)} />
  return <Modal title={`Quản lý phòng · ${cinema.name}`} onClose={onClose}>
    <div className="admin-room-manager">
      <p className="admin-form-note">Thay đổi lưu trên trình duyệt, chưa đồng bộ lên hệ thống rạp.</p>
      <form className="admin-form admin-room-create" onSubmit={submit}>
        <label>{editingRoom ? 'Đổi tên phòng' : 'Tên phòng mới'}<input required maxLength={100} value={name} onChange={event => setName(event.target.value)} placeholder="Ví dụ: Phòng 02" disabled={save.isPending} /></label>
        <div className="admin-room-actions"><button className="admin-button primary" disabled={save.isPending}><Icon name={editingRoom ? 'edit' : 'plus'} />{save.isPending ? 'Đang lưu…' : editingRoom ? 'Lưu tên phòng' : 'Thêm phòng'}</button>{editingRoom && <button type="button" className="admin-button" disabled={save.isPending} onClick={() => { setEditingRoom(null); setName(''); save.reset() }}>Hủy đổi tên</button>}</div>
      </form>
      {save.isError && <p className="admin-form-error" role="alert">{save.error.message}</p>}
      <div className="admin-room-list">{rooms.length ? rooms.map(room => {
        const seats = data.seats.filter(seat => seat.auditoriumId === room.id)
        return <article className="admin-room-card" key={room.id}>
          <div><h3>{room.name}</h3><p>{seats.length ? `${seats.filter(seat => seat.isActive).length} ghế sử dụng / ${seats.length} vị trí` : 'Chưa có sơ đồ ghế'}</p></div>
          <div className="admin-room-actions"><button className="admin-icon-button" disabled={save.isPending} aria-label={`Đổi tên ${room.name}`} onClick={() => { setEditingRoom(room); setName(room.name); save.reset() }}><Icon name="edit" /></button><button className="admin-button" disabled={save.isPending} aria-label={`Chỉnh sơ đồ ghế ${room.name}`} onClick={() => setLayoutRoom(room)}><Icon name="overview" />Chỉnh sơ đồ ghế</button></div>
        </article>
      }) : <p className="admin-form-note">Rạp chưa có phòng chiếu. Thêm phòng đầu tiên ở phía trên.</p>}</div>
      <div className="admin-dialog-actions"><button className="admin-button" onClick={onClose}>Đóng</button></div>
    </div>
  </Modal>
}

function SeatLayoutEditor({ room, data, onClose }: { room: Auditorium; data: AdminData; onClose: () => void }) {
  const queryClient = useQueryClient()
  const original = data.seats.filter(seat => seat.auditoriumId === room.id)
  const [seats, setSeats] = useState<PhysicalSeat[]>(original)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [selectionError, setSelectionError] = useState('')
  const [rowCount, setRowCount] = useState(new Set(original.map(seat => seat.rowLabel)).size || 6)
  const [columnCount, setColumnCount] = useState(original.length ? Math.max(...original.map(seat => seat.seatNumber)) : 10)
  const locked = data.showtimes.some(slot => slot.auditoriumId === room.id)
  const selectedSeats = seats.filter(seat => selectedIds.includes(seat.id))
  const selected = selectedSeats.length === 1 ? selectedSeats[0] : undefined
  const rows = [...new Set(seats.map(seat => seat.rowLabel))].sort()
  const columns = Math.min(30, Math.max(1, ...seats.map(seat => seat.seatNumber)))
  const dirty = JSON.stringify(seats) !== JSON.stringify(original)
  const save = useMutation({ mutationFn: adminService.saveSeats, onSuccess: result => {
    queryClient.setQueryData(['admin-demo'], result)
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
    setSelectedIds(current => ids.every(id => current.includes(id)) ? current.filter(id => !ids.includes(id)) : [...new Set([...current, ...ids])])
    setSelectionError('')
  }
  function applyType(type: SeatType) {
    if (locked || save.isPending) return
    try { setSeats(changeSeatType(seats, selectedIds, type)); setSelectedIds([]); setSelectionError(''); save.reset() }
    catch (error) { setSelectionError(error instanceof Error ? error.message : 'Không thể đổi loại ghế.') }
  }
  return <Modal title={`Chỉnh sơ đồ ghế · ${room.name}`} onClose={close}>
    <div className="admin-seat-editor">
      <p className="admin-form-note">Sơ đồ vật lý theo hàng và số ghế. Thay đổi lưu trên trình duyệt, chưa đồng bộ lên hệ thống rạp.</p>
      {locked && <p className="admin-form-error" role="status">Phòng đã có lịch chiếu, sơ đồ chỉ được xem. Tạo phòng mới để thiết kế sơ đồ khác.</p>}
      <form className="admin-form admin-seat-dimensions" onSubmit={generate}>
        <label>Số hàng<input type="number" min={1} max={26} required value={Number.isNaN(rowCount) ? '' : rowCount} onChange={event => setRowCount(event.target.valueAsNumber)} disabled={locked || save.isPending} /></label>
        <label>Ghế mỗi hàng<input type="number" min={1} max={30} required value={Number.isNaN(columnCount) ? '' : columnCount} onChange={event => setColumnCount(event.target.valueAsNumber)} disabled={locked || save.isPending} /></label>
        <button className="admin-button" disabled={locked || save.isPending}>{seats.length ? 'Tạo lại sơ đồ' : 'Tạo sơ đồ'}</button>
      </form>
      <div className="admin-seat-preview"><div className="admin-seat-screen">MÀN HÌNH</div><div className="admin-seat-scroll" role="group" aria-label={`Sơ đồ ghế ${room.name}`}>
        {rows.length ? rows.map(row => <div className="admin-seat-row" key={row}><span>{row || '?'}</span><div style={{ gridTemplateColumns: `repeat(${columns}, 36px)` }}>{Array.from({ length: columns }, (_, index) => {
          const seat = seats.find(item => item.rowLabel === row && item.seatNumber === index + 1)
          const partner = seats.find(item => item.id === seat?.pairedSeatId)
          return seat ? <button type="button" key={index} className={`admin-physical-seat ${seat.seatType.toLowerCase()} ${seat.isActive ? '' : 'inactive'} ${partner && seat.seatNumber < partner.seatNumber ? 'pair-start' : ''}`} title={`${seat.rowLabel}${seat.seatNumber} · ${seatTypes[seat.seatType]}`} aria-label={`${seat.rowLabel}${seat.seatNumber}, ghế ${seatTypes[seat.seatType]}${partner ? `, ghép với ${partner.rowLabel}${partner.seatNumber}` : ''}, ${seat.isActive ? 'sử dụng' : 'ngừng sử dụng'}`} aria-pressed={selectedIds.includes(seat.id)} disabled={save.isPending} onClick={() => toggleSeat(seat)}>{seat.isActive ? <>{seat.seatNumber}{seat.seatType === 'COUPLE' && <small>Đ</small>}</> : '×'}</button> : <span className="admin-seat-gap" key={index} />
        })}</div><span>{row || '?'}</span></div>) : <p className="admin-form-note">Chưa có ghế. Chọn số hàng và ghế mỗi hàng để tạo sơ đồ.</p>}
      </div></div>
      <div className="admin-seat-legend"><span>Thường</span><span>VIP · Vàng</span><span>Đ · Đôi (2 ghế)</span><span>× Ngừng sử dụng</span></div>
      {!!seats.length && <div className="admin-seat-batch"><p role="status">{selectedSeats.length ? `Đã chọn ${selectedSeats.length} ghế: ${selectedSeats.map(seat => `${seat.rowLabel}${seat.seatNumber}`).join(', ')}` : 'Bấm từng ghế để chọn nhiều ghế. Bấm lại để bỏ chọn.'}</p><div className="admin-room-actions"><button className="admin-button" disabled={locked || save.isPending || !selectedSeats.length} onClick={() => applyType('STANDARD')}>Đổi sang thường</button><button className="admin-button" disabled={locked || save.isPending || !selectedSeats.length} onClick={() => applyType('VIP')}>Đổi sang VIP</button><button className="admin-button" disabled={locked || save.isPending || !selectedSeats.length} onClick={() => applyType('COUPLE')}>Ghép ghế đôi</button><button className="admin-button" disabled={save.isPending || !selectedSeats.length} onClick={() => { setSelectedIds([]); setSelectionError('') }}>Bỏ chọn</button></div><p className="admin-form-note">Ghế đôi: chọn đúng 2 ghế liền nhau trong cùng hàng. Chọn một ghế đôi sẽ chọn cả cặp.</p></div>}
      {selectionError && <p className="admin-form-error" role="alert">{selectionError}</p>}
      {selected && <fieldset className="admin-form admin-seat-selection" disabled={locked || save.isPending}><legend>Ghế {selected.rowLabel}{selected.seatNumber}</legend><div className="admin-form-row"><label>Hàng ghế<input maxLength={1} pattern="[A-Za-z]" required value={selected.rowLabel} onChange={event => updateSelected({ rowLabel: event.target.value.toUpperCase() })} /></label><label>Số ghế<input type="number" min={1} max={30} required value={Number.isNaN(selected.seatNumber) ? '' : selected.seatNumber} onChange={event => updateSelected({ seatNumber: event.target.valueAsNumber })} /></label></div><label className="admin-seat-active"><input type="checkbox" checked={selected.isActive} onChange={event => updateSelected({ isActive: event.target.checked })} />Sử dụng ghế này</label></fieldset>}
      {save.isError && <p className="admin-form-error" role="alert">{save.error.message}</p>}
      <div className="admin-dialog-actions"><button className="admin-button" disabled={save.isPending} onClick={close}>Quay lại</button><button className="admin-button primary" disabled={locked || !dirty || !seats.length || save.isPending} onClick={() => save.mutate({ auditoriumId: room.id, seats })}>{save.isPending ? 'Đang lưu…' : 'Lưu sơ đồ'}</button></div>
    </div>
  </Modal>
}
