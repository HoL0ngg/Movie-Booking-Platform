import { useEffect, useRef } from 'react'

declare global { interface Window { google?: any } }

let initialized = false
let latestHandler: ((idToken: string) => void) | null = null // callback của component đang hiển thị, cấp module

export function GoogleSignInButton({ onCredential }: { onCredential: (idToken: string) => void }) {
  const boxRef = useRef<HTMLDivElement>(null)
  latestHandler = onCredential // mỗi lần render cập nhật; initialize() chỉ gọi qua biến này nên không bao giờ trỏ về component đã huỷ

  useEffect(() => {
    const box = boxRef.current
    if (!box) return
    const dark = document.documentElement.dataset.theme === 'dark'

    const render = () => {
      if (!window.google) return false
      if (!initialized) {
        window.google.accounts.id.initialize({
          client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
          callback: (res: { credential: string }) => latestHandler?.(res.credential), // luôn gọi handler mới nhất
        })
        initialized = true
      }
      box.innerHTML = ''
      window.google.accounts.id.renderButton(box, {
        type: 'standard', theme: dark ? 'filled_black' : 'outline', size: 'large',
        text: 'continue_with', shape: 'rectangular', logo_alignment: 'center',
        width: Math.min(400, Math.max(200, Math.floor(box.clientWidth))),
      })
      return true
    }

    let timer: number | undefined
    if (!render()) timer = window.setInterval(() => { if (render()) window.clearInterval(timer) }, 200)
    const ro = new ResizeObserver(() => { if (window.google) render() })
    ro.observe(box)
    return () => { window.clearInterval(timer); ro.disconnect() }
  }, [])

  return <div ref={boxRef} className="google-btn" aria-label="Tiếp tục với Google" />
}