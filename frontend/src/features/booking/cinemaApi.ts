import { getJson, HttpApiError } from '../../shared/apiClient'
import type { Cinema, Showtime } from '../../shared/contracts'
import { showtimeDays, showtimeDayKey } from './format'

// Dữ liệu thật từ cinema-service (qua gateway /api/v1).
interface CinemaDto { id: string; name: string; address: string; city: string; timezone: string }
interface ShowtimeDto {
  id: string; movieId: string; cinemaId: string; auditoriumId: string
  startsAt: string; endsAt: string; salesCloseAt: string
  price: { amountMinor: number; currency: string }
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED'
}

const toCinema = ({ id, name, address, city }: CinemaDto): Cinema => ({ id, name, address, city })

// Backend chưa có format (2D/IMAX) và language → dùng giá trị mặc định.
const toShowtime = (s: ShowtimeDto): Showtime => ({
  id: s.id, movieId: s.movieId, cinemaId: s.cinemaId, auditoriumId: s.auditoriumId,
  startsAt: s.startsAt, endsAt: s.endsAt,
  price: { amountMinor: s.price.amountMinor, currency: 'VND' },
  format: '2D', language: '',
})

async function listCinemas(signal?: AbortSignal) {
  return (await getJson<CinemaDto[]>('/cinemas', signal)).map(toCinema)
}

export const cinemaService = {
  list: () => listCinemas(),

  // Một request mỗi ngày cho mọi rạp, tránh nhân số request theo số rạp.
  async showtimes(movieId: string): Promise<Showtime[]> {
    const days = showtimeDays()
    const calls = days.map(day =>
      getJson<ShowtimeDto[]>(`/showtimes?date=${showtimeDayKey(day)}&movieId=${encodeURIComponent(movieId)}`))
    const all = (await Promise.all(calls)).flat()
    return all.map(toShowtime).sort((a, b) => Date.parse(a.startsAt) - Date.parse(b.startsAt))
  },

  async showtime(id: string): Promise<Showtime> {
    const s = await getJson<ShowtimeDto>(`/showtimes/${encodeURIComponent(id)}`)
    if (s.status === 'CANCELLED') throw new HttpApiError('SHOWTIME_CANCELLED', 'Suất chiếu đã bị hủy.', '', 410)
    return toShowtime(s)
  },
}
