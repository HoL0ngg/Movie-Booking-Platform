import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { useAdminAuth } from '../auth/AdminAuthContext'
import { Empty, Icon, Modal, PageHeading, Status } from './AdminUI'
import { movieAdminApi, movieAdminError, type AdminMovie, type Genre, type MovieInput } from './movieAdminApi'

export function MovieManager() {
  const { session } = useAdminAuth()
  const token = session!.token
  const client = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [editor, setEditor] = useState<{ movie?: AdminMovie } | null>(null)
  const [deleting, setDeleting] = useState<AdminMovie | null>(null)
  const [notice, setNotice] = useState('')
  const filter = searchParams.get('status') ?? ''
  const movies = useQuery({ queryKey: ['admin-movies', 'list'], queryFn: () => movieAdminApi.list(token), retry: false })
  const genres = useQuery({ queryKey: ['admin-movies', 'genres'], queryFn: () => movieAdminApi.genres(token), retry: false })
  const refresh = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: ['admin-movies'] }),
      client.invalidateQueries({ queryKey: ['movies'] }),
      client.invalidateQueries({ queryKey: ['movie'] }),
      client.invalidateQueries({ queryKey: ['cinema-admin', 'movies'] }),
    ])
  }
  const archive = useMutation({
    mutationFn: (id: string) => movieAdminApi.archive(token, id),
    onSuccess: async () => { await refresh(); setDeleting(null); setNotice('Đã xóa phim khỏi danh mục công khai. Dữ liệu được lưu trữ để giữ lịch sử.') },
  })
  const rows = (movies.data ?? []).filter(movie => (!filter || movie.status === filter)
    && movie.title.toLocaleLowerCase('vi-VN').includes(search.trim().toLocaleLowerCase('vi-VN')))
  const pages = Math.max(1, Math.ceil(rows.length / 8))
  const currentPage = Math.min(page, pages)
  const busy = archive.isPending
  return <>
    <PageHeading title="Quản lý phim" description="Danh mục phim, thông tin phát hành và trạng thái công bố.">
      <button className="admin-button primary" disabled={!genres.data || movies.isError || movies.isPending} onClick={() => setEditor({})}><Icon name="plus" />Thêm phim</button>
    </PageHeading>
    {notice && <p className="admin-success-notice" role="status">{notice}</p>}
    {genres.isError && <div role="alert"><p>{movieAdminError(genres.error)}</p><button className="admin-button" onClick={() => void genres.refetch()}>Tải lại thể loại</button></div>}
    <section className="admin-panel">
      <div className="admin-record-toolbar"><div className="admin-record-title"><h2>Danh sách phim</h2><span className="admin-count">{rows.length}</span></div><div className="admin-filters">
        <label className="admin-search"><Icon name="search" /><input type="search" aria-label="Tìm kiếm danh sách" placeholder="Tìm tên phim…" value={search} onChange={event => { setSearch(event.target.value); setPage(1) }} /></label>
        <select aria-label="Lọc danh sách" value={filter} onChange={event => { setPage(1); setSearchParams(event.target.value ? { status: event.target.value } : {}, { replace: true }) }}>
          <option value="">Tất cả trạng thái</option><option value="DRAFT">Bản nháp</option><option value="PUBLISHED">Đã công bố</option><option value="ARCHIVED">Đã lưu trữ</option>
        </select>
      </div></div>
      {movies.isPending ? <div className="admin-empty" role="status">Đang tải phim…</div> : movies.isError ? <div className="admin-empty" role="alert"><p>{movieAdminError(movies.error)}</p><button className="admin-button" onClick={() => void movies.refetch()}>Thử lại</button></div>
        : !rows.length ? <Empty /> : <div className="admin-table-scroll"><table className="admin-table"><thead><tr><th>Phim</th><th>Thể loại</th><th>Thời lượng</th><th>Khởi chiếu</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
          <tbody>{rows.slice((currentPage - 1) * 8, currentPage * 8).map(movie => <tr key={movie.id}>
            <td><strong>{movie.title}</strong></td><td>{movie.genreIds.map(id => genres.data?.find(genre => genre.id === id)?.name ?? '—').join(', ') || '—'}</td>
            <td>{movie.durationMinutes} phút</td><td>{movie.releaseDate ? new Date(`${movie.releaseDate}T00:00:00`).toLocaleDateString('vi-VN') : '—'}</td><td><Status value={movie.status} /></td>
            <td><div className="admin-room-actions"><button className="admin-button" disabled={!genres.data || busy} aria-label={`Chỉnh sửa ${movie.title}`} onClick={() => setEditor({ movie })}><Icon name="edit" />Chỉnh sửa</button>
              {movie.status !== 'ARCHIVED' && <button className="admin-button" disabled={busy} aria-label={`Xóa ${movie.title}`} onClick={() => { archive.reset(); setDeleting(movie) }}>Xóa</button>}</div></td>
          </tr>)}</tbody></table></div>}
      <div className="admin-pagination"><span>{rows.length} kết quả</span><div><button className="admin-button" disabled={currentPage <= 1} onClick={() => setPage(currentPage - 1)}>Trước</button><span>Trang {currentPage} / {pages}</span><button className="admin-button" disabled={currentPage >= pages} onClick={() => setPage(currentPage + 1)}>Sau</button></div></div>
    </section>
    {editor && <MovieEditor token={token} movie={editor.movie} genres={genres.data ?? []} onClose={() => setEditor(null)} onSaved={async () => { await refresh(); setNotice('Đã lưu thông tin phim.'); setEditor(null) }} />}
    {deleting && <Modal title="Xóa phim" onClose={() => { if (!busy) setDeleting(null) }}><div className="admin-form">
      <p>Xóa “{deleting.title}” khỏi danh mục công khai? Phim sẽ được lưu trữ; lịch chiếu và vé đã có được giữ nguyên.</p>
      {archive.error && <p className="admin-form-error" role="alert">{movieAdminError(archive.error)}</p>}
      <div className="admin-dialog-actions"><button className="admin-button" disabled={busy} onClick={() => setDeleting(null)}>Hủy</button><button className="admin-button primary" disabled={busy} onClick={() => archive.mutate(deleting.id)}>{busy ? 'Đang xóa…' : 'Xác nhận xóa'}</button></div>
    </div></Modal>}
  </>
}

function MovieEditor({ token, movie, genres, onClose, onSaved }: { token: string; movie?: AdminMovie; genres: Genre[]; onClose: () => void; onSaved: () => Promise<void> }) {
  const [genreError, setGenreError] = useState(false)
  const save = useMutation({ mutationFn: (input: MovieInput) => movie ? movieAdminApi.update(token, movie.id, input) : movieAdminApi.create(token, input), onSuccess: onSaved })
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (save.isPending) return
    const fields = new FormData(event.currentTarget)
    const genreIds = fields.getAll('genreIds').map(String)
    if (!genreIds.length) {
      setGenreError(true)
      event.currentTarget.querySelector<HTMLInputElement>('input[name="genreIds"]')?.focus()
      return
    }
    const text = (name: string) => String(fields.get(name) ?? '').trim()
    save.mutate({ title: text('title'), synopsis: text('synopsis') || null, posterUrl: text('posterUrl') || null,
      durationMinutes: Number(text('durationMinutes')), releaseDate: text('releaseDate') || new Date().toISOString().slice(0, 10),
      status: text('status') as MovieInput['status'], genreIds })
  }
  return <Modal title={movie ? 'Chỉnh sửa phim' : 'Thêm phim'} onClose={() => { if (!save.isPending) onClose() }}>
    <form className="admin-form" onSubmit={submit}>
      <label>Tên phim<input name="title" required maxLength={160} defaultValue={movie?.title} autoFocus /></label>
      <label>URL poster<input name="posterUrl" type="url" pattern="[hH][tT][tT][pP][sS]?://.*" maxLength={2048} defaultValue={movie?.posterUrl ?? ''} placeholder="https://…" /></label>
      <div className="admin-form-row"><label>Thời lượng (phút)<input name="durationMinutes" type="number" required min="1" max="600" step="1" defaultValue={movie?.durationMinutes ?? 120} /></label><label>Ngày khởi chiếu<input name="releaseDate" type="date" defaultValue={movie?.releaseDate ?? ''} /></label></div>
      <label>Trạng thái công bố<select name="status" defaultValue={movie?.status ?? 'DRAFT'}><option value="DRAFT">Bản nháp</option><option value="PUBLISHED">Đã công bố</option><option value="ARCHIVED">Đã lưu trữ</option></select></label>
      <fieldset className="admin-genre-selection" onChange={() => setGenreError(false)}>
        <legend>Thể loại (bắt buộc)</legend>
        <div className="admin-genre-options">{genres.map(genre => <label key={genre.id}>
          <input type="checkbox" name="genreIds" value={genre.id} defaultChecked={movie?.genreIds.includes(genre.id) ?? false} />{genre.name}
        </label>)}</div>
        {!genres.length && <p className="admin-form-error" role="alert">Chưa có thể loại. Cần có thể loại trước khi lưu phim.</p>}
        {genreError && <p className="admin-form-error" role="alert">Vui lòng chọn ít nhất một thể loại.</p>}
      </fieldset>
      <label>Tóm tắt nội dung<textarea name="synopsis" rows={3} maxLength={3000} defaultValue={movie?.synopsis ?? ''} /></label>
      {save.error && <p className="admin-form-error" role="alert">{movieAdminError(save.error)}</p>}
      <div className="admin-dialog-actions"><button type="button" className="admin-button" disabled={save.isPending} onClick={onClose}>Hủy</button><button className="admin-button primary" type="submit" disabled={save.isPending || !genres.length}>{save.isPending ? 'Đang lưu…' : 'Lưu thay đổi'}</button></div>
    </form>
  </Modal>
}
