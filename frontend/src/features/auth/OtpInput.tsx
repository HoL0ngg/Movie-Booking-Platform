import { useRef, type ClipboardEvent, type KeyboardEvent } from 'react'

const LENGTH = 6

export function OtpInput({ value, onChange, disabled }: { value: string; onChange: (v: string) => void; disabled?: boolean }) {
  const refs = useRef<(HTMLInputElement | null)[]>([])
  const digits = Array.from({ length: LENGTH }, (_, i) => value[i] ?? '')
  const focus = (i: number) => refs.current[Math.min(Math.max(i, 0), LENGTH - 1)]?.focus()

  const setDigit = (i: number, raw: string) => {
    const d = raw.replace(/\D/g, '')
    if (!d) return
    const next = digits.slice()
    let idx = i
    for (const ch of d) { if (idx >= LENGTH) break; next[idx++] = ch }
    onChange(next.join('').slice(0, LENGTH))
    focus(idx)
  }

  const onKeyDown = (i: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      e.preventDefault()
      const next = digits.slice()
      if (next[i]) next[i] = ''
      else if (i > 0) { next[i - 1] = ''; focus(i - 1) }
      onChange(next.join(''))
    } else if (e.key === 'ArrowLeft') { e.preventDefault(); focus(i - 1) }
    else if (e.key === 'ArrowRight') { e.preventDefault(); focus(i + 1) }
  }

  const onPaste = (e: ClipboardEvent<HTMLInputElement>) => {
    const d = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, LENGTH)
    if (!d) return
    e.preventDefault()
    onChange(d)
    focus(d.length)
  }

  return (
    <div className="otp-boxes" role="group" aria-label="Mã OTP 6 chữ số">
      {digits.map((d, i) => (
        <input key={i} ref={el => { refs.current[i] = el }} className="otp-box" inputMode="numeric" pattern="\d*"
          maxLength={LENGTH} autoComplete={i === 0 ? 'one-time-code' : 'off'} autoFocus={i === 0} disabled={disabled}
          aria-label={`Chữ số ${i + 1}`} value={d}
          onChange={e => setDigit(i, e.target.value)} onKeyDown={e => onKeyDown(i, e)} onPaste={onPaste}
          onFocus={e => e.target.select()} />
      ))}
    </div>
  )
}