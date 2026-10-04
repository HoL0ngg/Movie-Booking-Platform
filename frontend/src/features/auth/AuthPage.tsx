import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from './hooks'
import { showToast } from '../../shared/toast'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
const registerMode = mode === 'register'
const navigate = useNavigate()
const { login, register } = useAuth()
const action = registerMode ? register : login
const [email, setEmail] = useState('')
const [password, setPassword] = useState('')

const submit = (e: FormEvent) => {
e.preventDefault()
if (!email.trim()) return showToast('Vui lòng nhập email', 'error')
if (!/^\S+@\S+.\S+$/.test(email)) return showToast('Email không hợp lệ', 'error')
if (!password) return showToast('Vui lòng nhập mật khẩu', 'error')
if (registerMode && password.length < 8) return showToast('Mật khẩu tối thiểu 8 ký tự', 'error')

const options = { onSuccess: () => navigate('/bookings') }
registerMode
  ? register.mutate({ email, password }, options)
  : login.mutate({ email, password }, options)

}

return ( <div className="auth-page"> <div className="auth-visual"> <div> <p className="eyebrow">Một vé. Vạn cảm xúc.</p> <h1>Câu chuyện tiếp theo đang chờ bạn.</h1> </div> </div>

  <div className="auth-panel">
    <Link className="brand" to="/">
      <span>ciné</span>mat<i />
    </Link>

    <form onSubmit={submit} noValidate>
      <p className="eyebrow">
        {registerMode ? 'Thành viên mới' : 'Chào mừng trở lại'}
      </p>

      <h1>{registerMode ? 'Tạo tài khoản' : 'Đăng nhập'}</h1>

      <p>
        {registerMode
          ? 'Lưu vé và theo dõi lịch sử đặt chỗ.'
          : 'Tiếp tục hành trình điện ảnh của bạn.'}
      </p>

      <label>
        Email
        <input
          type="email"
          autoComplete="email"
          value={email}
          onChange={event => setEmail(event.target.value)}
        />
      </label>

      <label>
        Mật khẩu
        <input
          type="password"
          minLength={8}
          autoComplete={registerMode ? 'new-password' : 'current-password'}
          value={password}
          onChange={event => setPassword(event.target.value)}
        />
      </label>

      <button
        className="primary-button wide"
        disabled={action.isPending}
      >
        {action.isPending
          ? 'Đang xử lý…'
          : registerMode
            ? 'Tạo tài khoản'
            : 'Đăng nhập'}
      </button>

      <p className="switch-auth">
        {registerMode ? 'Đã có tài khoản?' : 'Chưa có tài khoản?'}{' '}
        <Link to={registerMode ? '/login' : '/register'}>
          {registerMode ? 'Đăng nhập' : 'Đăng ký'}
        </Link>
      </p>
    </form>
  </div>
</div>

)
}
