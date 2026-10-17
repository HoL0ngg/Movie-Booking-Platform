import { expect, test } from '@playwright/test'
import { mockAdminAuth, signInAdmin } from './admin-auth-fixture'

const id = '10000000-0000-0000-0000-000000000001'
const genreId = '20000000-0000-0000-0000-000000000001'
const secondGenreId = '20000000-0000-0000-0000-000000000002'
const genres = [{ id: genreId, name: 'Hành động' }, { id: secondGenreId, name: 'Phiêu lưu' }]
const movie = { id, title: 'Phim từ backend', synopsis: 'Nội dung', posterUrl: null, durationMinutes: 100, releaseDate: null, status: 'DRAFT', genreIds: [genreId] }

test('admin movies creates, updates and archives through authenticated API and refetches', async ({ page }) => {
  await page.clock.setFixedTime(new Date('2026-10-17T10:00:00Z'))
  await mockAdminAuth(page)
  let rows = [movie]
  let reads = 0
  const writes: { method: string; body: Record<string, unknown> | null }[] = []
  await page.route('**/api/v1/admin/genres', route => route.fulfill({ json: genres }))
  await page.route('**/api/v1/admin/movies**', async route => {
    expect(route.request().headers().authorization).toBe('Bearer test-only-admin-token')
    const method = route.request().method()
    if (method === 'GET') { reads++; return route.fulfill({ json: rows }) }
    const body = method === 'DELETE' ? null : route.request().postDataJSON()
    writes.push({ method, body })
    if (method === 'POST') {
      rows = [...rows, { ...body, id: '10000000-0000-0000-0000-000000000002' }]
      return route.fulfill({ status: 201, json: rows.at(-1) })
    }
    expect(route.request().url()).toContain(`/${id}`)
    if (method === 'PUT') { rows = rows.map(row => row.id === id ? { ...body, id } : row); return route.fulfill({ json: rows[0] }) }
    rows = rows.map(row => row.id === id ? { ...row, status: 'ARCHIVED' } : row)
    return route.fulfill({ status: 204 })
  })
  await page.goto('/admin/movies')
  await signInAdmin(page)
  await expect(page.getByRole('cell', { name: movie.title, exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Thêm phim', exact: true }).click()
  let dialog = page.getByRole('dialog')
  await dialog.getByLabel('Tên phim', { exact: true }).fill('Phim mới')
  await dialog.getByLabel('URL poster').fill('https://example.test/poster.jpg')
  await dialog.getByLabel('Ngày khởi chiếu').fill('2026-12-01')
  await dialog.getByLabel('Trạng thái công bố').selectOption('PUBLISHED')
  await dialog.getByRole('button', { name: 'Lưu thay đổi' }).click()
  await expect(dialog.getByRole('alert')).toHaveText('Vui lòng chọn ít nhất một thể loại.')
  await expect(dialog.getByRole('checkbox', { name: 'Hành động' })).toBeFocused()
  expect(writes).toHaveLength(0)
  await dialog.getByRole('checkbox', { name: 'Hành động' }).press('Space')
  await dialog.getByRole('checkbox', { name: 'Phiêu lưu' }).check()
  await expect(dialog.getByRole('alert')).toHaveCount(0)
  await dialog.getByRole('button', { name: 'Lưu thay đổi' }).click()
  await expect(dialog).not.toBeVisible()
  expect(writes[0]).toEqual({ method: 'POST', body: { title: 'Phim mới', synopsis: null, posterUrl: 'https://example.test/poster.jpg', durationMinutes: 120, releaseDate: '2026-12-01', status: 'PUBLISHED', genreIds: [genreId, secondGenreId] } })
  await expect(page.getByRole('cell', { name: 'Phim mới', exact: true })).toBeVisible()
  const published = page.getByRole('row').filter({ has: page.getByRole('cell', { name: 'Phim mới', exact: true }) })
  await expect(published.getByText('Đã công bố', { exact: true })).toHaveCSS('color', 'rgb(56, 124, 81)')
  await expect(published.getByRole('cell', { name: 'Hành động, Phiêu lưu', exact: true })).toBeVisible()
  await page.getByRole('button', { name: `Chỉnh sửa ${movie.title}`, exact: true }).click()
  dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('checkbox', { name: 'Hành động' })).toBeChecked()
  await expect(dialog.getByRole('checkbox', { name: 'Phiêu lưu' })).not.toBeChecked()
  await expect(dialog.getByLabel('Ngày khởi chiếu')).toHaveValue('')
  await dialog.getByLabel('Tên phim', { exact: true }).fill('Phim đã sửa')
  await dialog.getByRole('checkbox', { name: 'Hành động' }).uncheck()
  await dialog.getByRole('button', { name: 'Lưu thay đổi' }).click()
  await expect(dialog.getByRole('alert')).toHaveText('Vui lòng chọn ít nhất một thể loại.')
  expect(writes).toHaveLength(1)
  await dialog.getByRole('checkbox', { name: 'Phiêu lưu' }).check()
  await dialog.getByRole('button', { name: 'Lưu thay đổi' }).click()
  await expect(dialog).not.toBeVisible()
  expect(writes[1]).toEqual({ method: 'PUT', body: { title: 'Phim đã sửa', synopsis: 'Nội dung', posterUrl: null, durationMinutes: 100, releaseDate: '2026-10-17', status: 'DRAFT', genreIds: [secondGenreId] } })
  await page.getByRole('button', { name: 'Xóa Phim đã sửa', exact: true }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Hủy', exact: true }).click()
  expect(writes).toHaveLength(2)
  await page.getByRole('button', { name: 'Xóa Phim đã sửa', exact: true }).click()
  await page.getByRole('button', { name: 'Xác nhận xóa' }).click()
  await expect(page.getByRole('dialog')).not.toBeVisible()
  expect(writes[2]).toEqual({ method: 'DELETE', body: null })
  await page.getByRole('combobox', { name: 'Lọc danh sách' }).selectOption('ARCHIVED')
  await expect(page.getByRole('cell', { name: 'Phim đã sửa', exact: true })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'Phim mới', exact: true })).toHaveCount(0)
  await expect(page.getByRole('cell', { name: 'Đã lưu trữ', exact: true }).locator('.admin-status')).toHaveCSS('color', 'rgb(128, 98, 33)')
  expect(reads).toBeGreaterThanOrEqual(4)
  expect(await page.evaluate(() => localStorage.getItem('cinemat.admin-demo.v1'))).toBeNull()
})

test('creating a movie without a release date uses today', async ({ page }) => {
  await page.clock.setFixedTime(new Date('2026-10-17T23:30:00Z'))
  await mockAdminAuth(page)
  await page.route('**/api/v1/admin/genres', route => route.fulfill({ json: genres }))
  await page.route('**/api/v1/admin/movies', route => route.request().method() === 'GET'
    ? route.fulfill({ json: [] })
    : route.fulfill({ status: 201, json: { ...movie, ...route.request().postDataJSON() } }))
  await page.goto('/admin/movies')
  await signInAdmin(page)
  await page.getByRole('button', { name: 'Thêm phim', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('Tên phim', { exact: true }).fill('Phim không nhập ngày')
  await dialog.getByRole('checkbox', { name: 'Hành động' }).check()
  await expect(dialog.getByLabel('Ngày khởi chiếu')).toHaveValue('')
  const request = page.waitForRequest(request => request.url().endsWith('/api/v1/admin/movies') && request.method() === 'POST')
  await dialog.getByRole('button', { name: 'Lưu thay đổi' }).click()
  expect((await request).postDataJSON().releaseDate).toBe('2026-10-17')
  await expect(dialog).not.toBeVisible()
})

test('API failure retains the form and does not claim success', async ({ page }) => {
  await mockAdminAuth(page)
  await page.route('**/api/v1/admin/genres', route => route.fulfill({ json: genres }))
  await page.route('**/api/v1/admin/movies**', route => route.request().method() === 'GET'
    ? route.fulfill({ json: [movie] })
    : route.fulfill({ status: 403, json: { code: 'FORBIDDEN', traceId: 'test-movie-trace' } }))
  await page.goto('/admin/movies')
  await signInAdmin(page)
  await page.getByRole('button', { name: `Chỉnh sửa ${movie.title}`, exact: true }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Lưu thay đổi' }).click()
  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('Bạn không có quyền quản lý phim.')
  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('test-movie-trace')
  await expect(page.getByRole('dialog')).toBeVisible()
  await expect(page.getByText('Đã lưu thông tin phim.', { exact: true })).toHaveCount(0)
})

test('empty genre catalog explains why saving is unavailable', async ({ page }) => {
  await mockAdminAuth(page)
  await page.route('**/api/v1/admin/genres', route => route.fulfill({ json: [] }))
  await page.route('**/api/v1/admin/movies', route => route.fulfill({ json: [] }))
  await page.goto('/admin/movies')
  await signInAdmin(page)
  await page.getByRole('button', { name: 'Thêm phim', exact: true }).click()
  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('Chưa có thể loại.')
  await expect(page.getByRole('dialog').getByRole('button', { name: 'Lưu thay đổi' })).toBeDisabled()
})

test('failed reads allow retry and never display demo movies', async ({ page }) => {
  await mockAdminAuth(page)
  await page.route('**/api/v1/admin/genres', route => route.fulfill({ json: [] }))
  let failed = true
  await page.route('**/api/v1/admin/movies', route => failed
    ? route.fulfill({ status: 401, json: { code: 'AUTHENTICATION_REQUIRED' } })
    : route.fulfill({ json: [] }))
  await page.goto('/admin/movies')
  await signInAdmin(page)
  await expect(page.getByRole('alert')).toContainText('Phiên đăng nhập đã hết hạn')
  await expect(page.getByRole('button', { name: 'Thêm phim', exact: true })).toBeDisabled()
  failed = false
  await page.getByRole('button', { name: 'Thử lại', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Chưa có dữ liệu' })).toBeVisible()
})
