import { Route, Routes } from 'react-router-dom'
import { AppShell } from './AppShell'
import { AuthPage } from '../features/auth/AuthPage'
import { CheckoutPage } from '../features/booking/CheckoutPage'
import { HistoryPage } from '../features/booking/HistoryPage'
import { HomePage } from '../features/movies/HomePage'
import { MovieDetailPage } from '../features/movies/MovieDetailPage'
import { NotFoundPage } from './NotFoundPage'
import { SeatPage } from '../features/booking/SeatPage'
import { ShowtimesPage } from '../features/booking/ShowtimesPage'
import { SuccessPage } from '../features/booking/SuccessPage'
import { AdminLayout } from '../features/admin/AdminLayout'
import { AdminOverview, AdminRecords } from '../features/admin/AdminPages'
import { AdminLoginPage } from '../features/auth/AdminLoginPage'
import { RequireAdmin } from '../features/auth/AdminAuthContext'
import { RequireAuth } from '../features/auth/RequireAuth'

export default function App() {
  return <Routes>
    <Route path="admin/login" element={<AdminLoginPage />} />
    <Route element={<RequireAdmin />}>
      <Route path="admin" element={<AdminLayout />}>
        <Route index element={<AdminOverview />} />
        <Route path="movies" element={<AdminRecords key="movies" section="movies" />} />
        <Route path="cinemas" element={<AdminRecords key="cinemas" section="cinemas" />} />
        <Route path="showtimes" element={<AdminRecords key="showtimes" section="showtimes" />} />
        <Route path="bookings" element={<AdminRecords key="bookings" section="bookings" />} />
        <Route path="payments" element={<AdminRecords key="payments" section="payments" />} />
      </Route>
    </Route>
    <Route element={<AppShell />}>
      <Route index element={<HomePage />} />
      <Route path="movies/:movieId" element={<MovieDetailPage />} />
      <Route path="showtimes" element={<ShowtimesPage />} />
      <Route path="showtimes/:showtimeId/seats" element={<SeatPage />} />
      <Route element={<RequireAuth />}>
        <Route path="checkout" element={<CheckoutPage />} />
        <Route path="booking-success/:bookingId" element={<SuccessPage />} />
        <Route path="bookings" element={<HistoryPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Route>
    <Route path="login" element={<AuthPage mode="login" />} />
    <Route path="register" element={<AuthPage mode="register" />} />
  </Routes>
}
