import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from './hooks'
import { showToast } from '../../shared/toast'
import { ThemeToggle } from '../../app/ThemeToggle'
import { HttpApiError } from '../../shared/apiClient'
import { OtpInput } from './OtpInput'
import { PasswordInput } from './PasswordInput'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const registerMode = mode === 'register'
  const navigate = useNavigate()
  const { login, requestOtp, verifyOtp } = useAuth()
  const [step, setStep] = useState<'form' | 'otp'>('form')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [otp, setOtp] = useState('')
  const [cooldown, setCooldown] = useState(0) // giây còn lại trước khi được gửi lại

  useEffect(() => { // đếm ngược 1s/lần
    if (cooldown <= 0) return
    const t = window.setTimeout(() => setCooldown(c => c - 1), 1000)
    return () => window.clearTimeout(t)
  }, [cooldown])

  const sendOtp = () =>
  requestOtp.mutate({ email, password }, {
    onSuccess: c => { setStep('otp'); setOtp(''); setCooldown(c.resendAfter) },
    onError: e => {
      if (e instanceof HttpApiError && e.code === 'OTP_RESEND_TOO_SOON') { setStep('otp'); setCooldown(60) }
    },
  })

  const submitForm = (e: FormEvent) => { // B1: nhập email + mật khẩu
    e.preventDefault()
    if (!email.trim()) return showToast('Vui lòng nhập email', 'error')
    if (!/^\S+@\S+\.\S+$/.test(email)) return showToast('Email không hợp lệ', 'error')
    if (!password) return showToast('Vui lòng nhập mật khẩu', 'error')
    if (!registerMode) return login.mutate({ email, password }, { onSuccess: () => navigate('/bookings') }) // đăng nhập: không cần OTP
    if (password.length < 8) return showToast('Mật khẩu tối thiểu 8 ký tự', 'error')
    if (password !== confirm) return showToast('Mật khẩu xác nhận không khớp', 'error')
    sendOtp()
  }

  const submitOtp = (e: FormEvent) => { // B2: nhập mã
    e.preventDefault()
    if (!/^\d{6}$/.test(otp)) return showToast('Mã OTP gồm 6 chữ số', 'error')
    verifyOtp.mutate({ email, otp }, { onSuccess: () => navigate('/bookings') })
  }

  return (
    <div className="auth-page">
      <div className="auth-visual">
        <div>
          <p className="eyebrow">Một vé. Vạn cảm xúc.</p>
          <h1>Câu chuyện tiếp theo đang chờ bạn.</h1>
        </div>
      </div>

      <div className="auth-panel">
        <div className="auth-theme"><ThemeToggle /></div>
        <Link className="brand" to="/"><span>ciné</span>mat<i /></Link>

        {step === 'form' || !registerMode ? (
          <form onSubmit={submitForm} noValidate>
            <p className="eyebrow">{registerMode ? 'Thành viên mới' : 'Chào mừng trở lại'}</p>
            <h1>{registerMode ? 'Tạo tài khoản' : 'Đăng nhập'}</h1>
            <p>{registerMode ? 'Lưu vé và theo dõi lịch sử đặt chỗ.' : 'Tiếp tục hành trình điện ảnh của bạn.'}</p>
            <label>Email
              <input type="email" autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} />
            </label>
            <label>Mật khẩu
              <PasswordInput minLength={8} autoComplete={registerMode ? 'new-password' : 'current-password'}
                value={password} onChange={e => setPassword(e.target.value)} />
            </label>
            {registerMode && (
              <label>Xác nhận mật khẩu
                <PasswordInput autoComplete="new-password" value={confirm} onChange={e => setConfirm(e.target.value)} />
              </label>
            )}
            <button className="primary-button wide" disabled={requestOtp.isPending || login.isPending}>
              {registerMode ? (requestOtp.isPending ? 'Đang gửi mã…' : 'Tiếp tục') : (login.isPending ? 'Đang đăng nhập…' : 'Đăng nhập')}
            </button>
            <p className="switch-auth">
              {registerMode ? 'Đã có tài khoản?' : 'Chưa có tài khoản?'}{' '}
              <Link to={registerMode ? '/login' : '/register'}>{registerMode ? 'Đăng nhập' : 'Đăng ký'}</Link>
            </p>
          </form>
        ) : (
          <form onSubmit={submitOtp} noValidate>
            <p className="eyebrow">Xác thực email</p>
            <h1>Nhập mã OTP</h1>
            <p>Mã 6 số đã gửi tới <b>{email.trim()}</b>. Mã có hiệu lực 5 phút.</p>
            <OtpInput value={otp} onChange={setOtp} disabled={verifyOtp.isPending} />
            <button className="primary-button wide" disabled={verifyOtp.isPending}>
              {verifyOtp.isPending ? 'Đang xác thực…' : 'Xác nhận & tạo tài khoản'}
            </button>
            <p className="switch-auth">
              <button type="button" disabled={cooldown > 0 || requestOtp.isPending} onClick={sendOtp}>
                {cooldown > 0 ? `Gửi lại mã (${cooldown}s)` : 'Gửi lại mã'}
              </button>
              {' · '}
              <button type="button" onClick={() => setStep('form')}>Đổi email</button>
            </p>
          </form>
        )}
      </div>
    </div>
  )
}