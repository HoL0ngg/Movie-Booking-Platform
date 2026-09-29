import { Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { AuthPage } from './pages/AuthPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { HistoryPage } from './pages/HistoryPage'
import { HomePage } from './pages/HomePage'
import { MovieDetailPage } from './pages/MovieDetailPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { SeatPage } from './pages/SeatPage'
import { ShowtimesPage } from './pages/ShowtimesPage'
import { SuccessPage } from './pages/SuccessPage'

export default function App() {
  return <Routes>
    <Route element={<AppShell />}>
      <Route index element={<HomePage />} />
      <Route path="movies/:movieId" element={<MovieDetailPage />} />
      <Route path="showtimes" element={<ShowtimesPage />} />
      <Route path="showtimes/:showtimeId/seats" element={<SeatPage />} />
      <Route path="checkout" element={<CheckoutPage />} />
      <Route path="booking-success/:bookingId" element={<SuccessPage />} />
      <Route path="bookings" element={<HistoryPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Route>
    <Route path="login" element={<AuthPage mode="login" />} />
    <Route path="register" element={<AuthPage mode="register" />} />
  </Routes>
}
