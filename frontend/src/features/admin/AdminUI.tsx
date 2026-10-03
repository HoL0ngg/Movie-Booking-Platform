import { useEffect, useId, useRef, type ReactNode } from 'react'

export type IconName = 'overview' | 'movies' | 'cinemas' | 'showtimes' | 'bookings' | 'payments' | 'search' | 'arrow' | 'plus' | 'close' | 'menu' | 'edit'
const paths: Record<IconName, ReactNode> = {
  overview: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></>,
  movies: <><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M7 4v16M17 4v16M3 9h4m-4 6h4m10-6h4m-4 6h4" /></>,
  cinemas: <><path d="M4 21V7l8-4 8 4v14M2 21h20M9 21v-5h6v5M8 8h1m6 0h1M8 12h1m6 0h1" /></>,
  showtimes: <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 11h18M8 15h2m4 0h2m-8 3h2" /></>,
  bookings: <><path d="M3 7h18v4a2 2 0 0 0 0 4v4H3v-4a2 2 0 0 0 0-4V7Z" /><path d="M15 7v2m0 3v2m0 3v2" /></>,
  payments: <><rect x="2" y="5" width="20" height="14" rx="3" /><path d="M2 10h20M6 15h3" /></>,
  search: <><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 5 5" /></>,
  arrow: <path d="M5 12h14m-5-5 5 5-5 5" />,
  plus: <path d="M12 5v14M5 12h14" />,
  close: <path d="m6 6 12 12M6 18 18 6" />,
  menu: <path d="M4 6h16M4 12h16M4 18h16" />,
  edit: <><path d="m15 4 5 5M4 20l4-1L21 6l-4-4L4 15v5Z" /></>,
}
export function Icon({ name }: { name: IconName }) { return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg> }
export const money = (amount: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(amount)
export const dateTime = (value: string) => new Date(value).toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
const labels: Record<string, string> = { NOW_SHOWING: 'Đang chiếu', COMING_SOON: 'Sắp chiếu', CONFIRMED: 'Đã xác nhận', CANCELLED: 'Đã hủy', PAYMENT_PENDING: 'Chờ thanh toán', SUCCEEDED: 'Thành công', REQUESTED: 'Đang xử lý', FAILED: 'Thất bại' }
export function Status({ value }: { value: string }) { return <span className={`admin-status admin-status-${value.toLowerCase()}`}><span />{labels[value] ?? value}</span> }

export function Modal({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  useEffect(() => { const dialog = ref.current!; dialog.showModal(); return () => dialog.close() }, [])
  return <dialog className="admin-dialog" ref={ref} aria-labelledby={titleId} onCancel={event => { event.preventDefault(); onClose() }}>
    <div className="admin-dialog-heading"><div><span className="admin-overline">CINÉMAT WORKSPACE</span><h2 id={titleId}>{title}</h2></div><button type="button" className="admin-icon-button" onClick={onClose} aria-label="Đóng cửa sổ"><Icon name="close" /></button></div>
    {children}
  </dialog>
}

export function PageHeading({ title, description, children }: { title: string; description: string; children?: ReactNode }) {
  return <div className="admin-page-heading"><div><h1>{title}</h1><p>{description}</p></div><div className="admin-heading-actions">{children}</div></div>
}
export function Empty({ children = 'Không có kết quả phù hợp. Thử thay đổi từ khóa hoặc bộ lọc.' }: { children?: ReactNode }) {
  return <div className="admin-empty"><Icon name="search" /><h3>Chưa có dữ liệu</h3><p>{children}</p></div>
}
