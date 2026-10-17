import { expect, test } from '@playwright/test'
import { stubMovieApi } from './movie-api-fixture'

for (const viewport of [{ width: 1280, height: 800 }, { width: 375, height: 667 }]) {
  test('Cinemate scrolls and keeps the composer visible at ' + viewport.width + 'px', async ({ page }) => {
    await page.setViewportSize(viewport)
    await stubMovieApi(page)
    const reply = 'Một gợi ý phim rất dài. '.repeat(150) + 'x'.repeat(400)
    await page.route('**/api/v1/ai/chat', route => route.fulfill({ json: { message: reply } }))
    await page.goto('/')
    await page.getByRole('button', { name: 'Mở Cinemate' }).click()
    const input = page.getByRole('textbox', { name: 'Nhập tin nhắn cho Cinemate' })
    await input.fill('Gợi ý phim giúp tôi')
    await page.getByRole('button', { name: 'Gửi tin nhắn' }).click()
    await expect(page.locator('.cinemate-message.is-assistant')).toHaveText(reply)
    await expect(input).toBeEnabled()
    await expect(input).toBeInViewport({ ratio: 1 })
    await expect(page.getByRole('button', { name: 'Gửi tin nhắn' })).toBeInViewport({ ratio: 1 })
    const list = page.locator('.cinemate-message-list')
    expect(await list.evaluate(el => el.scrollHeight > el.clientHeight)).toBe(true)
    expect(await list.evaluate(el => el.scrollWidth <= el.clientWidth)).toBe(true)
    await list.hover()
    await page.mouse.wheel(0, 500)
    await expect.poll(() => list.evaluate(el => el.scrollTop)).toBeGreaterThan(0)
    await expect(input).toBeInViewport({ ratio: 1 })
    await input.fill('Tôi vẫn nhập được tin nhắn')
    const offset = await page.locator('.cinemate-avatar').evaluate(el => {
      const avatar = el.getBoundingClientRect()
      const icon = el.querySelector('svg')!.getBoundingClientRect()
      return { x: Math.abs(icon.x + icon.width / 2 - avatar.x - avatar.width / 2), y: Math.abs(icon.y + icon.height / 2 - avatar.y - avatar.height / 2) }
    })
    expect(offset.x).toBeLessThan(1)
    expect(offset.y).toBeLessThan(1)
  })
}
