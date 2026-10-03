import { expect, test } from '@playwright/test'
import { stubMovieApi } from './movie-api-fixture'

test('customer can complete the mock booking flow', async ({ page }) => {
  await stubMovieApi(page)
  await page.goto('/')
  await page.getByRole('link', { name: /đặt vé ngay/i }).click()
  await page.getByRole('link', { name: /10:/ }).first().click()
  await page.getByRole('button', { name: /A1, ghế thường, còn trống/i }).click()
  await page.getByRole('link', { name: /tiếp tục/i }).click()
  await page.getByRole('button', { name: /thanh toán/i }).click()
  await expect(page.getByRole('heading', { name: /hẹn bạn tại rạp/i })).toBeVisible()
  await page.getByRole('link', { name: /xem vé của tôi/i }).click()
  await expect(page.getByText(/đã xác nhận/i).first()).toBeVisible()
})
