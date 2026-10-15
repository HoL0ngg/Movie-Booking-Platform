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
    await route.fulfill({ json: cinemaId ? [{
      id: `${cinemaId}-${date}`, movieId: url.searchParams.get('movieId'), cinemaId,
      auditoriumId: 'room-1', startsAt: `${date}T20:00:00Z`, endsAt: `${date}T22:00:00Z`,
      salesCloseAt: `${date}T19:45:00Z`, price: { amountMinor: 95000, currency: 'VND' }, status: 'PUBLISHED',
    }] : cinemas })
  })
  await page.goto('/showtimes')
})

for (const mobile of [false, true]) {
  test(`movie, cinema and date filters update showtimes on ${mobile ? 'mobile' : 'desktop'}`, async ({ page }) => {
    await page.setViewportSize(mobile ? { width: 390, height: 844 } : { width: 1280, height: 900 })
    const filters = page.getByRole('region', { name: 'Chọn suất chiếu', exact: true })
    await filters.locator('summary').filter({ hasText: movies[0].title }).click()
    await page.getByRole('searchbox', { name: 'Tìm phim', exact: true }).fill(movies[1].title)
    await expect(page.getByRole('region', { name: 'Danh sách phim', exact: true }).getByRole('button', { pressed: false })).toHaveCount(1)
    await page.getByRole('button', { name: `${movies[1].title} 118 phút · T16`, exact: true }).click()
    await expect(page).toHaveURL(/movieId=movie-2/)
    await expect(page.getByRole('region', { name: 'Danh sách phim', exact: true })).toBeHidden()

    await filters.locator('summary').filter({ hasText: 'Tất cả rạp' }).click()
    await page.getByRole('searchbox', { name: 'Tìm rạp', exact: true }).fill('Đồng Khởi')
    await page.getByRole('button', { name: `${cinemas[1].name} ${cinemas[1].address}`, exact: true }).click()
    await expect(page.locator('.cinema-row')).toHaveCount(1)
    await expect(page.locator('.cinema-row h2')).toHaveText(cinemas[1].name)

    await page.getByRole('button', { name: 'Ngày mai 16/10', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Ngày mai 16/10', exact: true })).toHaveAttribute('aria-pressed', 'true')
    await expect(page.locator('.time-grid a')).toHaveAttribute('href', '/showtimes/cinema-2-2026-10-16/seats')
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
    await page.screenshot({ path: `test-results/showtime-filters-${mobile ? 'mobile' : 'desktop'}.png`, fullPage: true })
  })
}

test('pickers support keyboard selection, dismissal and empty search', async ({ page }) => {
  const trigger = page.locator('.showtime-picker summary').first()
  await trigger.focus()
  await page.keyboard.press('Enter')
  const search = page.getByRole('searchbox', { name: 'Tìm phim', exact: true })
  await expect(search).toBeFocused()
  await search.fill('không có phim này')
  await expect(page.getByRole('status')).toHaveText('Không tìm thấy kết quả phù hợp.')
  await page.keyboard.press('Escape')
  await expect(trigger).toBeFocused()
  await expect(search).toBeHidden()
  await page.keyboard.press('Enter')
  await expect(search).toHaveValue('')
  await search.fill(movies[1].title)
  await page.keyboard.press('Tab')
  await page.keyboard.press('Enter')
  await expect(page).toHaveURL(/movieId=movie-2/)
  await expect(trigger).toBeFocused()
  await page.keyboard.press('Enter')
  await page.getByRole('heading', { name: 'Chọn trải nghiệm của bạn' }).click()
  await expect(search).toBeHidden()
})
