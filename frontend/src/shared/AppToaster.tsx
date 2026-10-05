import toast, { Toaster, ToastBar } from 'react-hot-toast'

export function AppToaster() {
  return <Toaster position="top-center" containerStyle={{ top: 100, right: 16, left: 16 }}
    toastOptions={{
      className: 'app-toast',
      duration: 4500,
      style: {
        background: 'var(--surface)', color: 'var(--paper)',
        border: '1px solid var(--line)', borderRadius: 12, padding: '14px 16px',
        maxWidth: 'min(420px, calc(100vw - 32px))',
        boxShadow: '0 12px 40px rgba(0, 0, 0, .15)',
      },
      success: { duration: 4000, iconTheme: { primary: 'var(--success)', secondary: 'var(--surface)' } },
      error: { duration: 6500, iconTheme: { primary: 'var(--accent)', secondary: 'var(--surface)' } },
    }}>
    {notification => <ToastBar toast={notification}>
      {({ icon, message }) => <>
        <span className={`toast-icon toast-icon-${notification.type}`} aria-hidden="true">
          {icon ?? <span className="toast-info-icon">i</span>}
        </span>
        <div className="toast-copy">
          <strong>{notification.type === 'error' ? 'Có lỗi xảy ra' : notification.type === 'success' ? 'Thành công' : 'Thông báo'}</strong>
          {message}
        </div>
        <button type="button" className="toast-close" aria-label="Đóng thông báo" onClick={() => toast.dismiss(notification.id)}>
          <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="m6 6 12 12M18 6 6 18" /></svg>
        </button>
      </>}
    </ToastBar>}
  </Toaster>
}
