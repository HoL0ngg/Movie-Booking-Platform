import { cinemaErrorMessage } from './cinemaAdminApi'

export function CinemaLoadError({ error, retry }: { error: unknown; retry: () => void }) {
  return <div className="admin-empty" role="alert"><p>{cinemaErrorMessage(error)}</p><button className="admin-button" onClick={retry}>Thử lại</button></div>
}
