import type { SeatType } from '../../shared/contracts'

export interface PhysicalSeat { id: string; auditoriumId: string; rowLabel: string; seatNumber: number; seatType: SeatType; isActive: boolean; pairedSeatId?: string }

export function changeSeatType(seats: PhysicalSeat[], selectedIds: string[], type: SeatType): PhysicalSeat[] {
  const selected = seats.filter(seat => selectedIds.includes(seat.id))
  if (!selected.length) throw new Error('Chọn ghế trước khi đổi loại.')
  if (selected.some(seat => seat.pairedSeatId && !selectedIds.includes(seat.pairedSeatId))) throw new Error('Hãy chọn cả hai ghế trong cặp để đổi loại.')
  if (type === 'COUPLE' && (selected.length !== 2 || selected[0].rowLabel !== selected[1].rowLabel || Math.abs(selected[0].seatNumber - selected[1].seatNumber) !== 1 || selected.some(seat => !seat.isActive))) throw new Error('Ghế đôi cần đúng 2 ghế đang sử dụng, liền nhau trong cùng hàng.')
  return seats.map(seat => selectedIds.includes(seat.id) ? { ...seat, seatType: type, pairedSeatId: type === 'COUPLE' ? selected.find(item => item.id !== seat.id)!.id : undefined } : seat)
}
