import { expect, test } from '@playwright/test'

const movie = {
  id: '0e55f5aa-f7c0-48cb-bcd9-6a36fbeae8e2', title: 'Phim từ API',
  synopsis: null, posterUrl: null, durationMinutes: 101, releaseDate: null, status: 'NOW_SHOWING',
}

test('catalog and detail read HTTP data with missing optional metadata', async ({ page }) => {
  const requests: string[] = []
  await page.route('**/api/v1/movies**', async route => {
    const url = new URL(route.request().url())
    requests.push(url.pathname + url.search)
    await route.fulfill({ json: url.pathname.endsWith(movie.id) ? movie : url.searchParams.get('status') === 'COMING_SOON' ? [] : [movie] })
  })
  await page.goto('/')
  await expect(page.getByRole('status')).toContainText('Chưa có phim sắp chiếu.')
  await page.getByRole('link', { name: `Xem chi tiết ${movie.title}` }).click()
  await expect(page.getByRole('heading', { name: movie.title })).toBeVisible()
  await expect(page.getByAltText(`Poster ${movie.title}`)).toHaveAttribute('src', '/movie-placeholder.svg')
  expect(requests).toContain('/api/v1/movies?status=NOW_SHOWING')
  expect(requests).toContain('/api/v1/movies?status=COMING_SOON')
  expect(requests).toContain(`/api/v1/movies/${movie.id}`)
})

test('catalog and detail use the poster URL from the API', async ({ page }) => {
  const posterUrl = '/movie-placeholder.svg?api-poster'
  const withPoster = { ...movie, posterUrl }
  await page.route('**/api/v1/movies**', route => route.fulfill({
    json: new URL(route.request().url()).pathname.endsWith(movie.id) ? withPoster : [withPoster],
  }))
  await page.goto('/')
  const card = page.getByRole('link', { name: `Xem chi tiết ${movie.title}` }).first()
  await expect(card.getByRole('img')).toHaveAttribute('src', posterUrl)
  await card.click()
  await expect(page.getByAltText(`Poster ${movie.title}`)).toHaveAttribute('src', posterUrl)
})

test('catalog shows a request failure and supports retry without mock fallback', async ({ page }) => {
  let failing = true
  await page.route('**/api/v1/movies**', route => route.fulfill(failing
    ? { status: 503, json: { code: 'DEPENDENCY_UNAVAILABLE', message: 'Unavailable', traceId: 'test-trace' } }
    : { json: [movie] }))
  await page.goto('/')
  await expect(page.getByRole('alert').first()).toContainText('Không thể tải')
  await expect(page.getByRole('link', { name: /Xem chi tiết/ })).toHaveCount(0)
  failing = false
  await page.getByRole('button', { name: 'Thử lại' }).first().click()
  await expect(page.getByRole('link', { name: `Xem chi tiết ${movie.title}` })).toBeVisible()
})

test('missing movie displays a not-found error', async ({ page }) => {
  await page.route('**/api/v1/movies/**', route => route.fulfill({ status: 404, json: { code: 'MOVIE_NOT_FOUND', message: 'Movie not found.', traceId: 'test-trace' } }))
  await page.goto(`/movies/${movie.id}`)
  await expect(page.getByRole('alert')).toHaveText('Không tìm thấy phim.')
})
