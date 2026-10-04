export function LoadingState({ label = 'Đang tải dữ liệu…' }: { label?: string }) {
  return <div className="state-card" role="status"><span className="spinner" />{label}</div>
}

export function ErrorState({ message }: { message: string }) {
  return <div className="state-card error" role="alert">{message}</div>
}
