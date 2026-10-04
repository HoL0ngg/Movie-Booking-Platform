import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from './hooks'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const registerMode = mode === 'register'
  const navigate = useNavigate()
  const { login, register } = useAuth()
  const action = registerMode ? register : login
  const [name, setName] = useState('')
  const [email, setEmail] = useState('demo@cinemat.vn')
  const [password, setPassword] = useState('123456')
  const submit = (event: FormEvent) => { event.preventDefault(); const options = { onSuccess: () => navigate('/bookings') }; if (registerMode) register.mutate({ name, email, password }, options); else login.mutate({ email, password }, options) }
  return <div className="auth-page"><div className="auth-visual"><div><p className="eyebrow">Một vé. Vạn cảm xúc.</p><h1>Câu chuyện tiếp theo đang chờ bạn.</h1></div></div><div className="auth-panel"><Link className="brand" to="/"><span>ciné</span>mat<i /></Link><form onSubmit={submit}><p className="eyebrow">{registerMode ? 'Thành viên mới' : 'Chào mừng trở lại'}</p><h1>{registerMode ? 'Tạo tài khoản' : 'Đăng nhập'}</h1><p>{registerMode ? 'Lưu vé và theo dõi lịch sử đặt chỗ.' : 'Tiếp tục hành trình điện ảnh của bạn.'}</p>{registerMode && <label>Họ và tên<input autoComplete="name" value={name} onChange={event => setName(event.target.value)} required /></label>}<label>Email<input type="email" autoComplete="email" value={email} onChange={event => setEmail(event.target.value)} required /></label><label>Mật khẩu<input type="password" minLength={6} autoComplete={registerMode ? 'new-password' : 'current-password'} value={password} onChange={event => setPassword(event.target.value)} required /></label>{action.error && <p className="form-error" role="alert">{action.error.message}</p>}<button className="primary-button wide" disabled={action.isPending}>{action.isPending ? 'Đang xử lý…' : registerMode ? 'Tạo tài khoản' : 'Đăng nhập'}</button><p className="switch-auth">{registerMode ? 'Đã có tài khoản?' : 'Chưa có tài khoản?'} <Link to={registerMode ? '/login' : '/register'}>{registerMode ? 'Đăng nhập' : 'Đăng ký'}</Link></p></form></div></div>
}
