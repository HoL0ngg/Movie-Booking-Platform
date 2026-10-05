import { useState } from 'react'

export function ThemeToggle() {
  const [dark, setDark] = useState(() => document.documentElement.dataset.theme === 'dark')

  function toggleTheme() {
    const next = !dark
    const theme = next ? 'dark' : 'light'
    document.documentElement.dataset.theme = theme
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', next ? '#161922' : '#f7f8fc')
    try { localStorage.setItem('cinemat-theme', theme) } catch { /* Theme still works without storage. */ }
    setDark(next)
  }

  return <button type="button" className="theme-toggle" onClick={toggleTheme}
    aria-label="Chế độ tối" aria-pressed={dark} title={dark ? 'Chuyển sang chế độ sáng' : 'Chuyển sang chế độ tối'}>
    <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      {dark ? <><circle cx="12" cy="12" r="4" /><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5" /></> : <path d="M20.5 13A8.5 8.5 0 0 1 11 3.5 8.5 8.5 0 1 0 20.5 13Z" />}
    </svg>
    <span>{dark ? 'Sáng' : 'Tối'}</span>
  </button>
}
