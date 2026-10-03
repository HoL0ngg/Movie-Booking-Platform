import { expect, test } from '@playwright/test'
import { movies } from '../src/mocks/data'

test('movie card links to its details with an accessible label', async ({ page }) => {
  await page.goto('/')
  const movie = movies[0]
  const card = page.getByRole('link', { name: `Xem chi tiết ${movie.title}` })
  await expect(card).toHaveAttribute('href', `/movies/${movie.id}`)
  await card.click()
  await expect(page).toHaveURL(new RegExp(`/movies/${movie.id}$`))
  await expect(page.getByRole('heading', { name: movie.title })).toBeVisible()
})
