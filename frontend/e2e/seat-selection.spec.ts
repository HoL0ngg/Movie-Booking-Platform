import { expect, test } from '@playwright/test'
import { cinemas, showtimes } from '../src/shared/mocks/data'
import { stubMovieApi } from './movie-api-fixture'

for (const theme of ['light', 'dark']) {
  test(`selected seats keep their numbers and unavailable seats show a large cross in ${theme} mode`, async ({ page }) => {
    await page.addInitScript(value => {
      localStorage.setItem('cinemat-theme', value)
    }, theme)
    await stubMovieApi(page)
    const showtime = showtimes[0]
    await page.route('**/api/v1/showtimes/**', route => route.fulfill({ json: { ...showtime, status: 'PUBLISHED' } }))
    await page.route('**/api/v1/cinemas', route => route.fulfill({ json: cinemas }))
    await page.goto(`/showtimes/${showtime.id}/seats`)
    await expect(page.getByRole('heading', { name: 'Dune: Hành Tinh Cát', exact: true })).toBeVisible()
    await expect(page.locator('html')).toHaveAttribute('data-theme', theme)

    const regular = page.getByRole('button', { name: /^A1, ghế thường,/ })
    const vip = page.getByRole('button', { name: /^E1, ghế VIP,/ })
    const unavailable = page.getByRole('button', { name: 'A3, ghế thường, không khả dụng', exact: true })
    const selectedColor = await page.locator('.seat-demo.selected').evaluate(element => getComputedStyle(element).backgroundColor)

    for (const seat of [regular, vip]) {
      await seat.hover()
      await seat.click()
      await expect(seat).toHaveAttribute('aria-pressed', 'true')
      await expect(seat).toHaveCSS('background-color', selectedColor)
      await expect.poll(() => seat.evaluate(element => getComputedStyle(element, '::after').content)).toBe('none')
      await expect(seat.locator('span')).toHaveCSS('visibility', 'visible')
      await expect(seat.locator('span')).toHaveText('1')
      await page.getByRole('heading', { name: 'Dune: Hành Tinh Cát', exact: true }).hover()
      await expect(seat).toHaveCSS('background-color', selectedColor)
      await seat.hover()
      await expect(seat).toHaveCSS('background-color', selectedColor)
    }

    await expect(page.locator('.booking-bar')).toContainText('A1, E1')
    await expect(unavailable).toBeDisabled()
    await expect.poll(() => unavailable.evaluate(element => getComputedStyle(element, '::after').content)).toBe('"×"')
    expect(await unavailable.evaluate(element => parseFloat(getComputedStyle(element, '::after').fontSize))).toBeGreaterThanOrEqual(24)
    await expect(unavailable.locator('span')).toHaveCSS('visibility', 'hidden')
    const disabledColor = await unavailable.evaluate(element => getComputedStyle(element).backgroundColor)
    await unavailable.hover()
    await expect(unavailable).toHaveCSS('background-color', disabledColor)
    await expect(unavailable).toHaveAttribute('aria-pressed', 'false')
    await regular.click()
    await expect(regular).toHaveAttribute('aria-pressed', 'false')
    await expect(regular.locator('span')).toHaveCSS('visibility', 'visible')
    await expect.poll(() => regular.evaluate(element => getComputedStyle(element, '::after').content)).toBe('none')
    await vip.focus()
    await page.keyboard.press('Space')
    await expect(vip).toHaveAttribute('aria-pressed', 'false')
    await expect(vip).toHaveCSS('outline-width', '3px')
    await page.keyboard.press('Space')
    await expect(vip).toHaveAttribute('aria-pressed', 'true')
    await page.screenshot({ path: `test-results/seat-selection-${theme}.png`, fullPage: true })
  })
}
