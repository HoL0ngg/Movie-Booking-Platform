import { expect, type Page } from '@playwright/test'
import type { AdminCinema, ShowtimeDto } from '../src/features/admin/cinemaAdminApi'
import type { Auditorium } from '../src/shared/contracts'
import type { PhysicalSeat } from '../src/features/admin/seatLayout'

export async function mockCinemaAdmin(page: Page, locked = false) {
  const cinemaId = '10000000-0000-0000-0000-000000000001'
  const roomId = '20000000-0000-0000-0000-000000000001'
  const movieId = '30000000-0000-0000-0000-000000000001'
  const state = {
    cinemas: [{ id: cinemaId, name: 'Rạp thử nghiệm', city: 'Hồ Chí Minh', address: 'Địa chỉ thử nghiệm', timezone: 'Asia/Ho_Chi_Minh' }] as AdminCinema[],
    rooms: [{ id: roomId, cinemaId, name: 'Phòng thử nghiệm' }] as Auditorium[],
    seats: ['A', 'B'].flatMap(rowLabel => [1, 2, 3].map(seatNumber => ({
      id: `${rowLabel}${seatNumber}`, auditoriumId: roomId, rowLabel, seatNumber,
      seatType: rowLabel === 'A' && seatNumber < 3 ? 'COUPLE' : 'STANDARD', isActive: true,
      ...(rowLabel === 'A' && seatNumber < 3 ? { pairedSeatId: seatNumber === 1 ? 'A2' : 'A1' } : {}),
    }))) as PhysicalSeat[],
    showtimes: (locked ? [{ id: 'slot-existing', cinemaId, auditoriumId: roomId, movieId, startsAt: '2027-01-01T10:00:00Z', endsAt: '2027-01-01T12:00:00Z', salesCloseAt: '2027-01-01T10:00:00Z', status: 'PUBLISHED', price: { amountMinor: 95000, currency: 'VND' } }] : []) as ShowtimeDto[],
    writes: [] as { path: string; method: string; body: Record<string, unknown> }[],
  }
  await page.route('**/api/v1/admin/movies', route => route.fulfill({ json: [{ id: movieId, title: 'Phim thật', synopsis: '', posterUrl: null, durationMinutes: 120, releaseDate: '2026-01-01', status: 'PUBLISHED', genreIds: [] }] }))
  await page.route('**/api/v1/admin/genres', route => route.fulfill({ json: [] }))
  await page.route(/\/api\/v1\/(admin\/(cinemas|auditoriums|showtimes)|showtimes)(\/|\?|$)/, route => {
    expect(route.request().headers().authorization).toBe('Bearer test-only-admin-token')
    const url = new URL(route.request().url())
    const path = url.pathname.replace('/api/v1', '')
    const method = route.request().method()
    const parts = path.split('/')
    if (method === 'GET') {
      if (path === '/admin/cinemas') return route.fulfill({ json: state.cinemas })
      if (path.endsWith('/auditoriums')) return route.fulfill({ json: state.rooms.filter(r => r.cinemaId === parts[3]) })
      if (path.endsWith('/seats')) return route.fulfill({ json: state.seats.filter(s => s.auditoriumId === parts[3]) })
      return route.fulfill({ json: state.showtimes.filter(s => !url.searchParams.get('cinemaId') || s.cinemaId === url.searchParams.get('cinemaId')) })
    }
    const body = route.request().postData() ? route.request().postDataJSON() : {}
    state.writes.push({ path, method, body })
    if (path === '/admin/cinemas' || /^\/admin\/cinemas\/[^/]+$/.test(path)) {
      const cinema = { ...body, id: method === 'POST' ? `cinema-${state.cinemas.length}` : parts[3] } as AdminCinema
      state.cinemas = [...state.cinemas.filter(c => c.id !== cinema.id), cinema]
      return route.fulfill({ status: method === 'POST' ? 201 : 200, json: cinema })
    }
    if (path.endsWith('/seats')) {
      state.seats = [...state.seats.filter(s => s.auditoriumId !== parts[3]), ...body.seats]
      return route.fulfill({ json: body.seats })
    }
    if (path.endsWith('/auditoriums') || /^\/admin\/auditoriums\/[^/]+$/.test(path)) {
      const room = method === 'POST' ? { id: `room-${state.rooms.length}`, cinemaId: parts[3], name: body.name } : { ...state.rooms.find(r => r.id === parts[3])!, name: body.name }
      state.rooms = [...state.rooms.filter(r => r.id !== room.id), room]
      return route.fulfill({ status: method === 'POST' ? 201 : 200, json: room })
    }
    if (path.endsWith('/publish') || path.endsWith('/cancel')) {
      const slot = state.showtimes.find(s => s.id === parts[2])!
      slot.status = path.endsWith('/publish') ? 'PUBLISHED' : 'CANCELLED'
      return route.fulfill({ json: slot })
    }
    const room = state.rooms.find(r => r.id === body.auditoriumId)!
    const slot = { ...body, cinemaId: room.cinemaId, id: method === 'POST' ? `slot-${state.showtimes.length}` : parts[3], status: 'DRAFT', price: { amountMinor: body.priceMinor, currency: body.currency } } as ShowtimeDto
    state.showtimes = [...state.showtimes.filter(s => s.id !== slot.id), slot]
    return route.fulfill({ status: method === 'POST' ? 201 : 200, json: slot })
  })
  return state
}
