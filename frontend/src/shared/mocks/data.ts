import type { Auditorium, Booking, Cinema, Movie, Seat, Showtime } from '../contracts'

const image = (id: string, width = 900) => `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${width}&q=85`

export const movies: Movie[] = [
  { id: 'movie-1', title: 'Dune: Hành Tinh Cát', originalTitle: 'Dune: Part Two', synopsis: 'Paul Atreides hợp lực với Chani và người Fremen trong hành trình trả thù, đối mặt lựa chọn giữa tình yêu và vận mệnh của vũ trụ.', posterUrl: image('photo-1500530855697-b586d89ba3ee', 600), backdropUrl: image('photo-1446776811953-b23d57bd21aa', 1800), genres: ['Khoa học viễn tưởng', 'Phiêu lưu'], durationMinutes: 166, ageRating: 'T13', releaseDate: '2026-09-12', rating: 8.8, status: 'NOW_SHOWING', director: 'Denis Villeneuve', cast: ['Timothée Chalamet', 'Zendaya', 'Rebecca Ferguson'] },
  { id: 'movie-2', title: 'Thành Phố Trong Mưa', originalTitle: 'City in the Rain', synopsis: 'Hai người xa lạ tìm thấy nhau trong một đêm Sài Gòn mưa lớn, nơi mọi con đường đều dẫn về những ký ức chưa kịp nói.', posterUrl: image('photo-1519608487953-e999c86e7455', 600), backdropUrl: image('photo-1519608487953-e999c86e7455', 1800), genres: ['Tình cảm', 'Chính kịch'], durationMinutes: 118, ageRating: 'T16', releaseDate: '2026-09-20', rating: 8.1, status: 'NOW_SHOWING', director: 'Nguyễn Minh', cast: ['Khánh Linh', 'Hoàng Sơn'] },
  { id: 'movie-3', title: 'Vùng Tối', originalTitle: 'The Dark Reach', synopsis: 'Một đội thám hiểm bước vào vùng tín hiệu bí ẩn và phát hiện rằng thứ đáng sợ nhất không nằm ngoài không gian.', posterUrl: image('photo-1462331940025-496dfbfc7564', 600), backdropUrl: image('photo-1462331940025-496dfbfc7564', 1800), genres: ['Kinh dị', 'Viễn tưởng'], durationMinutes: 127, ageRating: 'T18', releaseDate: '2026-09-08', rating: 7.9, status: 'NOW_SHOWING', director: 'Mara Cole', cast: ['Noah Reid', 'Mia Chen'] },
  { id: 'movie-4', title: 'Kẻ Trộm Ánh Trăng', originalTitle: 'Moonlight Thief', synopsis: 'Một phi vụ bất khả thi giữa thành phố tương lai mở ra bí mật đã bị chôn giấu nhiều năm.', posterUrl: image('photo-1516339901601-2e1b62dc0c45', 600), backdropUrl: image('photo-1516339901601-2e1b62dc0c45', 1800), genres: ['Hành động', 'Tội phạm'], durationMinutes: 132, ageRating: 'T16', releaseDate: '2026-10-10', rating: 8.4, status: 'COMING_SOON', director: 'Alex Tran', cast: ['Lena Park', 'James Vu'] },
  { id: 'movie-5', title: 'Mùa Hè Cuối Cùng', originalTitle: 'Our Last Summer', synopsis: 'Nhóm bạn cũ trở lại thị trấn ven biển cho mùa hè cuối trước khi mỗi người đi một hướng.', posterUrl: image('photo-1507525428034-b723cf961d3e', 600), backdropUrl: image('photo-1507525428034-b723cf961d3e', 1800), genres: ['Tuổi trẻ', 'Gia đình'], durationMinutes: 104, ageRating: 'P', releaseDate: '2026-10-24', rating: 7.8, status: 'COMING_SOON', director: 'Lê An', cast: ['Mai Anh', 'Minh Khoa'] },
]

export const cinemas: Cinema[] = [
  { id: 'cinema-1', name: 'Cinémat Landmark', address: '208 Nguyễn Hữu Cảnh, Bình Thạnh', city: 'TP. Hồ Chí Minh' },
  { id: 'cinema-2', name: 'Cinémat Đồng Khởi', address: '72 Lê Thánh Tôn, Quận 1', city: 'TP. Hồ Chí Minh' },
  { id: 'cinema-3', name: 'Cinémat West Lake', address: '17 Xuân Diệu, Tây Hồ', city: 'Hà Nội' },
]

export const auditoriums: Auditorium[] = cinemas.map((cinema, index) => ({ id: `aud-${index + 1}`, cinemaId: cinema.id, name: index === 0 ? 'IMAX Hall' : `Screen ${index + 1}` }))

const at = (days: number, hour: number, minute = 0) => {
  const date = new Date()
  date.setHours(hour, minute, 0, 0)
  date.setDate(date.getDate() + days)
  return date.toISOString()
}

export const showtimes: Showtime[] = movies.filter(movie => movie.status === 'NOW_SHOWING').flatMap((movie, movieIndex) =>
  cinemas.flatMap((cinema, cinemaIndex) => [0, 1, 2].map((days) => {
    const startsAt = at(days, 10 + movieIndex * 2 + cinemaIndex, days * 15)
    return { id: `${movie.id}-${cinema.id}-${days}`, movieId: movie.id, cinemaId: cinema.id, auditoriumId: auditoriums[cinemaIndex].id, startsAt, endsAt: new Date(new Date(startsAt).getTime() + movie.durationMinutes * 60_000).toISOString(), price: { amountMinor: cinemaIndex === 0 ? 120_000 : 95_000, currency: 'VND' }, format: cinemaIndex === 0 ? 'IMAX' : '2D', language: 'Phụ đề Việt' }
  }))
)

export const seatsFor = (showtimeId: string): Seat[] => {
  const showtime = showtimes.find(item => item.id === showtimeId)
  const base = showtime?.price.amountMinor ?? 95_000
  const blocked = new Set(['A3', 'B7', 'C4', 'C5', 'D9', 'E2', 'F6'])
  return ['A', 'B', 'C', 'D', 'E', 'F'].flatMap((row, rowIndex) =>
    Array.from({ length: 10 }, (_, index) => {
      const label = `${row}${index + 1}`
      const type = rowIndex >= 4 ? 'VIP' as const : 'STANDARD' as const
      return { id: `${showtimeId}-${label}`, showtimeId, row, number: index + 1, label, type, status: blocked.has(label) ? 'SOLD' as const : 'AVAILABLE' as const, price: { amountMinor: base + (type === 'VIP' ? 25_000 : 0), currency: 'VND' as const } }
    })
  )
}

export const seededBooking: Booking = {
  id: 'booking-past', reservationId: 'reservation-past', userId: 'user-demo', movie: { id: movies[1].id, title: movies[1].title, posterUrl: movies[1].posterUrl }, cinema: { id: cinemas[1].id, name: cinemas[1].name, address: cinemas[1].address }, showtime: showtimes.find(item => item.movieId === 'movie-2' && item.cinemaId === 'cinema-2')!, items: [{ id: 'item-past-1', seatId: 'seat-past', seatLabel: 'D5', price: { amountMinor: 95_000, currency: 'VND' } }], total: { amountMinor: 95_000, currency: 'VND' }, status: 'CONFIRMED', bookingCode: 'CINE2409', createdAt: new Date(Date.now() - 86_400_000 * 4).toISOString(),
}
