import { Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { AuthPage } from './pages/auth/AuthPage'
import { CheckoutPage } from './pages/payment/CheckoutPage'
import { HistoryPage } from './pages/profile/HistoryPage'
import { HomePage } from './pages/home/HomePage'
import { MovieDetailPage } from './pages/movies/MovieDetailPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { SeatPage } from './pages/cinemas/SeatPage'
import { ShowtimesPage } from './pages/booking/ShowtimesPage'
import { SuccessPage } from './pages/payment/SuccessPage'
import { AdminLayout } from './features/admin/AdminLayout'
import { AdminOverview, AdminRecords } from './features/admin/AdminPages'

export default function App() {
  return <Routes>
    <Route path="admin" element={<AdminLayout />}>
      <Route index element={<AdminOverview />} />
      <Route path="movies" element={<AdminRecords key="movies" section="movies" />} />
      <Route path="cinemas" element={<AdminRecords key="cinemas" section="cinemas" />} />
      <Route path="showtimes" element={<AdminRecords key="showtimes" section="showtimes" />} />
      <Route path="bookings" element={<AdminRecords key="bookings" section="bookings" />} />
      <Route path="payments" element={<AdminRecords key="payments" section="payments" />} />
    </Route>
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
