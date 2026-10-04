import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { HttpApiError } from '../../shared/apiClient'
import { Icon } from '../admin/AdminUI'
import { useAdminAuth } from './AdminAuthContext'
import { adminAuthApi, adminAuthError } from './api'
import '../admin/admin.css'
import './admin-login.css'

export function AdminLoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const { establish } = useAdminAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from: unknown = location.state?.from
  const destination = typeof from === 'string' && /^\/admin(?:\/(?:movies|cinemas|showtimes|bookings|payments))?(?:\?[^#]*)?$/.test(from) ? from : '/admin'
  const login = useMutation({
    retry: false,
    mutationFn: async () => {
      const startedAt = Date.now()
      const token = await adminAuthApi.login(email, password)
      const expiresAt = startedAt + token.expiresIn * 1000
      const profile = await adminAuthApi.profile(token.accessToken)
      if (expiresAt <= Date.now()) throw new Error('TOKEN_EXPIRED')
      return { token, profile, expiresAt }
    },
    onSuccess: ({ token, profile, expiresAt }) => {
      setPassword('')
      establish(token, profile, expiresAt)
      navigate(destination, { replace: true })
    },
  })
  const submit = (event: FormEvent) => { event.preventDefault(); if (!login.isPending) login.mutate() }
  return <main className="admin-shell admin-login">
    <section className="admin-login-panel">
      <Link to="/" className="admin-logo"><span className="admin-logo-mark"><Icon name="movies" /></span><span>cinémat</span></Link>
      <h1>Đăng nhập quản trị</h1>
        <form onSubmit={submit} aria-busy={login.isPending}>
          <label htmlFor="admin-email">Email công việc</label><input id="admin-email" type="email" autoComplete="username" placeholder="ten@congty.vn" value={email} onChange={event => { setEmail(event.target.value); login.reset() }} required maxLength={254} disabled={login.isPending} />
          <label htmlFor="admin-password">Mật khẩu</label><div className="admin-login-password"><input id="admin-password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" placeholder="Nhập mật khẩu của bạn" value={password} onChange={event => { setPassword(event.target.value); login.reset() }} required maxLength={128} disabled={login.isPending} /><button type="button" aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={showPassword} onClick={() => setShowPassword(!showPassword)}>{showPassword ? 'Ẩn' : 'Hiện'}</button></div>
          {login.error && <div className="admin-login-error" role="alert">{adminAuthError(login.error)}{login.error instanceof HttpApiError && login.error.traceId && <small>Mã hỗ trợ: {login.error.traceId}</small>}</div>}
          <button className="admin-button primary admin-login-submit" disabled={login.isPending}>{login.isPending ? 'Đang xác thực…' : 'Đăng nhập'}<Icon name="arrow" /></button>
        </form>
      <p className="admin-login-support">Quên mật khẩu? Liên hệ quản trị hệ thống.</p>
      <Link to="/" className="admin-login-back">← Về trang khách hàng</Link>
    </section>
  </main>
}
