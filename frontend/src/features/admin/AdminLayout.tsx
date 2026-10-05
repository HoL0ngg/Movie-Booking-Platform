import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { adminService, type AdminData } from './adminService'
import { Icon, type IconName } from './AdminUI'
import './admin.css'
import { useAdminAuth, useAdminProfile } from '../auth/AdminAuthContext'

export const adminNavigation: { path: string; label: string; icon: IconName }[] = [
  { path: '', label: 'Tổng quan', icon: 'overview' },
  { path: 'movies', label: 'Phim', icon: 'movies' },
  { path: 'cinemas', label: 'Rạp chiếu phim', icon: 'cinemas' },
  { path: 'showtimes', label: 'Lịch chiếu', icon: 'showtimes' },
  { path: 'bookings', label: 'Đặt vé', icon: 'bookings' },
  { path: 'payments', label: 'Thanh toán', icon: 'payments' },
]
export function AdminLayout() {
  const { logout } = useAdminAuth()
  const { data: profile } = useAdminProfile()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const section = adminNavigation.find(item => location.pathname === `/admin${item.path ? `/${item.path}` : ''}`)?.label ?? 'Quản trị'
  const query = useQuery({ queryKey: ['admin-demo'], queryFn: adminService.list })
  return <div className="admin-shell">
    <a className="admin-skip" href="#admin-content">Chuyển đến nội dung</a>
    <aside className={`admin-sidebar ${menuOpen ? 'is-open' : ''}`}>
      <NavLink to="/admin" className="admin-logo"><span className="admin-logo-mark"><Icon name="movies" /></span><span>cinémat<span className="admin-logo-sub">QUẢN TRỊ</span></span></NavLink>
      <span className="admin-nav-label">QUẢN LÝ VẬN HÀNH</span>
      <nav aria-label="Điều hướng quản trị">{adminNavigation.map(item => <NavLink key={item.path} end to={`/admin${item.path ? `/${item.path}` : ''}`} onClick={() => setMenuOpen(false)}><Icon name={item.icon} /><span>{item.label}</span></NavLink>)}</nav>
      <div className="admin-sidebar-bottom"><NavLink className="admin-customer-link" to="/">Mở trang khách hàng <Icon name="arrow" /></NavLink><div className="admin-identity"><span className="admin-avatar">AD</span><div><strong>{profile?.email}</strong><small>Quản trị viên</small></div></div></div>
    </aside>
    <div className="admin-main">
      <header className="admin-topbar"><div className="admin-breadcrumb"><button className="admin-icon-button admin-menu-button" aria-label="Mở menu quản trị" aria-expanded={menuOpen} onClick={() => setMenuOpen(!menuOpen)}><Icon name="menu" /></button><strong>{section}</strong></div><div className="admin-topbar-right"><span className="admin-today">{new Date().toLocaleDateString('vi-VN', { day: '2-digit', month: 'long', year: 'numeric' })}</span><button className="admin-button" onClick={logout}>Đăng xuất</button></div></header>
      <main id="admin-content" className="admin-content" tabIndex={-1}>
        {query.isPending ? <div className="admin-empty" role="status">Đang tải không gian quản trị…</div> : query.isError ? <div className="admin-empty" role="alert"><h2>Không thể tải dữ liệu</h2><p>{query.error.message}</p><button className="admin-button" onClick={() => void query.refetch()}>Thử lại</button></div> : <Outlet context={query.data satisfies AdminData} />}
        <div className="admin-page-footer"><span>© {new Date().getFullYear()} Cinémat</span><span></span></div>
      </main>
    </div>
  </div>
}
