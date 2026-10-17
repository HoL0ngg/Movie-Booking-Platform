import { useEffect, useId, useState } from 'react'
import type { FormEvent } from 'react'
import { request } from '../shared/apiClient'

type ChatRole = 'USER' | 'ASSISTANT'

type ChatMessage = {
  role: ChatRole
  content: string
}

type ChatResponse = {
  message: string
}

export function CinemateChat() {
  const [isOpen, setIsOpen] = useState(false)
  const [message, setMessage] = useState('')
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [isSending, setIsSending] = useState(false)
  const [error, setError] = useState('')
  const titleId = useId()

  useEffect(() => {
    if (!isOpen) {
      return
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const content = message.trim()
    if (!content || isSending) return

    setMessage('')
    setError('')
    setIsSending(true)
    const userMessage: ChatMessage = { role: 'USER', content }
    setMessages((current) => [...current, userMessage])

    try {
      const response = await request<ChatResponse>('/ai/chat', {
        method: 'POST',
        body: JSON.stringify({ message: content, history: messages }),
      })
      setMessages((current) => [...current, { role: 'ASSISTANT', content: response.message }])
    } catch {
      setError('Cinemate hiện chưa thể trả lời. Vui lòng thử lại sau.')
    } finally {
      setIsSending(false)
    }
  }

  return (
    <div className="cinemate-chat">
      {isOpen && (
        <section id={`${titleId}-panel`} className="cinemate-panel" role="dialog" aria-modal="false" aria-labelledby={titleId}>
          <header className="cinemate-panel-header">
            <div className="cinemate-avatar" aria-hidden="true">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <circle cx="12" cy="12" r="9" />
                <circle cx="9" cy="9" r=".75" fill="currentColor" stroke="none" />
                <circle cx="15" cy="9" r=".75" fill="currentColor" stroke="none" />
                <path d="M8 14c1 4 7 4 8 0" strokeLinecap="round" />
              </svg>
            </div>
            <div>
              <h2 id={titleId}>Cine<em className="cinemate-mate">mate</em></h2>
              <p>Người bạn AI mê phim của bạn</p>
            </div>
            <button
              className="cinemate-close"
              type="button"
              onClick={() => setIsOpen(false)}
              aria-label="Đóng Cinemate"
            >
              <span aria-hidden="true">×</span>
            </button>
          </header>

          <div className="cinemate-messages" aria-live="polite">
            {messages.length === 0 ? (
              <div className="cinemate-empty-state">
                <span className="cinemate-empty-icon" aria-hidden="true">✦</span>
                <p>Chào bạn, mình là Cine<em className="cinemate-mate">mate</em>!</p>
                <span>Kể mình nghe tâm trạng hôm nay, cùng tìm một bộ phim hợp gu nhé.</span>
              </div>
            ) : (
              <div className="cinemate-message-list">
                {messages.map((item, index) => (
                  <div className={`cinemate-message ${item.role === 'USER' ? 'is-user' : 'is-assistant'}`} key={`${item.role}-${index}`}>
                    {item.content}
                  </div>
                ))}
                {isSending && <div className="cinemate-message is-assistant">Cinemate đang suy nghĩ...</div>}
              </div>
            )}
          </div>

          <form className="cinemate-composer" onSubmit={handleSubmit}>
            <label className="sr-only" htmlFor="cinemate-message">
              Nhập tin nhắn cho Cinemate
            </label>
            <input
              id="cinemate-message"
              type="text"
              placeholder="Kể mình nghe gu phim của bạn..."
              autoComplete="off"
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              disabled={isSending}
            />
            <button className="cinemate-send" type="submit" aria-label="Gửi tin nhắn" disabled={isSending || !message.trim()}>
              <span aria-hidden="true">↑</span>
            </button>
          </form>
          {error && <p className="cinemate-error" role="alert">{error}</p>}
        </section>
      )}

      <button
        className="cinemate-launcher"
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        aria-expanded={isOpen}
        aria-controls={`${titleId}-panel`}
        aria-label={isOpen ? 'Đóng Cinemate' : 'Mở Cinemate'}
      >
        <span className="cinemate-launcher-icon" aria-hidden="true">✦</span>
        <span>Cine<em className="cinemate-mate">mate</em></span>
      </button>
    </div>
  )
}
