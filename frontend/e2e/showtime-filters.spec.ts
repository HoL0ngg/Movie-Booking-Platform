import { expect, test } from '@playwright/test'
import { cinemas, movies } from '../src/shared/mocks/data'
import { stubMovieApi } from './movie-api-fixture'

test.use({ timezoneId: 'UTC' })

test.beforeEach(async ({ page }) => {
  await page.clock.install({ time: new Date('2026-10-15T12:00:00Z') })
  await stubMovieApi(page)
  await page.route('**/api/v1/cinemas**', async route => {
    const url = new URL(route.request().url())
    const cinemaId = url.pathname.split('/cinemas/')[1]?.split('/')[0]
    const date = url.searchParams.get('date')
    const movieId = url.searchParams.get('movieId')
    await route.fulfill({ json: cinemaId ? [{
      id: `${movieId}-${cinemaId}-${date}`, movieId, cinemaId,
      auditoriumId: 'room-1', startsAt: `${date}T20:00:00Z`, endsAt: `${date}T22:00:00Z`,
      salesCloseAt: `${date}T19:45:00Z`, price: { amountMinor: 95000, currency: 'VND' }, status: 'PUBLISHED',
    }] : cinemas })
  })
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
