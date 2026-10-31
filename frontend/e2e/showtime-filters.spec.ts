import { expect, test } from '@playwright/test'
import { cinemas, movies } from '../src/shared/mocks/data'
import { stubMovieApi } from './movie-api-fixture'

test.use({ timezoneId: 'UTC' })

test.beforeEach(async ({ page }) => {
  await page.clock.install({ time: new Date('2026-10-15T12:00:00Z') })
  await stubMovieApi(page)
  await page.route('**/api/v1/cinemas', route => route.fulfill({ json: cinemas }))
  await page.route('**/api/v1/showtimes?**', async route => {
    const url = new URL(route.request().url())
    const date = url.searchParams.get('date')
    const movieId = url.searchParams.get('movieId')
    await route.fulfill({ json: cinemas.map(cinema => ({
      id: `${movieId}-${cinema.id}-${date}`, movieId, cinemaId: cinema.id,
      auditoriumId: 'room-1', startsAt: `${date}T13:00:00Z`, endsAt: `${date}T15:00:00Z`,
      salesCloseAt: `${date}T12:45:00Z`, price: { amountMinor: 95000, currency: 'VND' }, status: 'PUBLISHED',
    })) })
  })
})

test('loads eight daily schedules without multiplying requests by cinema count', async ({ page }) => {
  const requests: string[] = []
  page.on('request', request => {
    if (/\/api\/v1\/(cinemas|showtimes)/.test(request.url())) requests.push(request.url())
  })
  await page.goto('/showtimes?movieId=movie-1')
  await expect(page.locator('.time-grid a')).toHaveCount(cinemas.length)
  const schedules = requests.filter(url => new URL(url).pathname === '/api/v1/showtimes')
  expect(schedules).toHaveLength(8)
  expect(new Set(schedules.map(url => new URL(url).searchParams.get('date'))).size).toBe(8)
  expect(schedules.every(url => new URL(url).searchParams.get('movieId') === 'movie-1')).toBe(true)
  expect(requests.some(url => /\/cinemas\/[^/]+\/showtimes/.test(url))).toBe(false)
})

for (const mobile of [false, true]) {
  test(`selected movie shows dates and showtimes grouped by cinema on ${mobile ? 'mobile' : 'desktop'}`, async ({ page }) => {
    await page.setViewportSize(mobile ? { width: 390, height: 844 } : { width: 1280, height: 900 })
    await page.goto('/')
    await page.getByRole('link', { name: `Đặt vé ${movies[1].title}`, exact: true }).click()
    await expect(page).toHaveURL(/showtimes\?movieId=movie-2$/)
    await expect(page.getByRole('heading', { name: movies[1].title, exact: true })).toBeVisible()
    await expect(page.getByText(movies[1].synopsis, { exact: true })).toBeVisible()
    await expect(page.getByRole('link', { name: /Chi tiết phim/ })).toHaveCount(0)
    await expect(page.getByText('Chọn ngày xem, sau đó chọn giờ chiếu tại rạp phù hợp.', { exact: true })).toHaveCount(0)
    await expect(page.getByRole('combobox')).toHaveCount(0)
    await expect(page.locator('.cinema-row')).toHaveCount(cinemas.length)
    for (const cinema of cinemas) {
      const row = page.getByRole('article').filter({ has: page.getByRole('heading', { name: cinema.name, exact: true }) })
      await expect(row.getByRole('link')).toHaveAttribute('href', `/showtimes/movie-2-${cinema.id}-2026-10-15/seats`)
    }
    await page.getByRole('button', { name: /^Ngày mai / }).click()
    await expect(page.getByRole('button', { name: /^Ngày mai / })).toHaveAttribute('aria-pressed', 'true')
    for (const cinema of cinemas) {
      await expect(page.locator(`.time-grid a[href="/showtimes/movie-2-${cinema.id}-2026-10-16/seats"]`)).toBeVisible()
    }
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
    const dates = page.getByRole('group', { name: 'Lịch chiếu' }).getByRole('button')
    await expect(dates).toHaveCount(8)
    await dates.last().click()
    await expect(dates.last()).toHaveAttribute('aria-pressed', 'true')
    for (const cinema of cinemas) {
      await expect(page.locator(`.time-grid a[href="/showtimes/movie-2-${cinema.id}-2026-10-22/seats"]`)).toBeVisible()
    }
    await page.screenshot({ path: `test-results/showtime-filters-${mobile ? 'mobile' : 'desktop'}.png`, fullPage: true })
    await page.reload()
    await expect(page.getByRole('heading', { name: movies[1].title, exact: true })).toBeVisible()
  })
}

test('schedule entry asks for a movie and date controls support the keyboard', async ({ page }) => {
  await page.goto('/showtimes')
  await expect(page.getByRole('heading', { name: 'Bạn muốn xem phim gì?' })).toBeVisible()
  await expect(page.locator('.cinema-row')).toHaveCount(0)
  await page.getByRole('link', { name: `Đặt vé ${movies[0].title}`, exact: true }).click()
  const tomorrow = page.getByRole('button', { name: /^Ngày mai / })
  await tomorrow.focus()
  await page.keyboard.press('Enter')
  await expect(tomorrow).toHaveAttribute('aria-pressed', 'true')
  await expect(page.locator('.time-grid a').first()).toHaveAttribute('href', '/showtimes/movie-1-cinema-1-2026-10-16/seats')
  await page.getByRole('link', { name: '← Chọn phim khác', exact: true }).click()
  await page.getByRole('link', { name: `Đặt vé ${movies[1].title}`, exact: true }).click()
  await expect(page.getByRole('heading', { name: movies[1].title, exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: /^Hôm nay / })).toHaveAttribute('aria-pressed', 'true')
})

test('dates use Ho Chi Minh time even before UTC midnight', async ({ page }) => {
  await page.clock.setFixedTime(new Date('2026-10-15T18:00:00Z'))
  await page.goto('/showtimes?movieId=movie-1')
  await expect(page.getByRole('button', { name: /^Hôm nay 16[/-]10/ })).toHaveAttribute('aria-pressed', 'true')
  await expect(page.locator('.time-grid a').first()).toHaveAttribute('href', '/showtimes/movie-1-cinema-1-2026-10-16/seats')
  await page.getByRole('group', { name: 'Lịch chiếu' }).getByRole('button').last().click()
  await expect(page.locator('.time-grid a').first()).toHaveAttribute('href', '/showtimes/movie-1-cinema-1-2026-10-23/seats')
})
