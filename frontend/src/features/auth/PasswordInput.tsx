import { useState, type InputHTMLAttributes } from 'react'

export function EyeIcon({ off }: { off: boolean }) { // off = đang hiện mật khẩu → gạch chéo con mắt
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z" />
      <circle cx="12" cy="12" r="3" />
      {off && <path d="M3 3l18 18" />}
    </svg>
  )
}

export function PasswordInput(props: Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>) {
  const [show, setShow] = useState(false)
  return (
    <div className="password-field">
      <input {...props} type={show ? 'text' : 'password'} />
      <button type="button" className="password-toggle" aria-pressed={show}
        aria-label={show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} onClick={() => setShow(s => !s)}>
        <EyeIcon off={show} />
      </button>
    </div>
  )
}