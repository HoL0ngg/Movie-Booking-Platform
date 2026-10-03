import { NavLink, Outlet } from 'react-router-dom'
import { useAuth, useCurrentUser } from '../hooks/useApi'

export function AppShell() {
  const { data: user } = useCurrentUser()
  const { logout } = useAuth()
  return <div className="app-shell">
    <header className="site-header">
      <NavLink className="brand" to="/" aria-label="Cinémat home"><span>ciné</span>mat<i /></NavLink>
      <nav aria-label="Điều hướng chính">
        <NavLink to="/">Phim</NavLink>
        <NavLink to="/showtimes">Lịch chiếu</NavLink>
        <NavLink to="/bookings">Vé của tôi</NavLink>
      </nav>
      <div className="header-actions">
        {user ? <><span className="welcome">Chào, {user.name.split(' ')[0]}</span><button className="ghost-button" onClick={() => logout.mutate()}>Đăng xuất</button></> : <NavLink className="ghost-button" to="/login">Đăng nhập</NavLink>}
      </div>
    </header>
    <main><Outlet /></main>
    <footer><div className="brand muted"><span>ciné</span>mat</div><p>Điện ảnh. Theo cách của bạn.</p><p>Prototype sử dụng dữ liệu mô phỏng. · <NavLink to="/admin">Quản trị demo</NavLink></p></footer>
  </div>
}
