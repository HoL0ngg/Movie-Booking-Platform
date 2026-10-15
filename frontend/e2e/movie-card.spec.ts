import { expect, test } from '@playwright/test'
import { movies } from '../src/shared/mocks/data'
import { stubMovieApi } from './movie-api-fixture'

test.beforeEach(async ({ page }) => {
  await stubMovieApi(page)
  await page.route('**/api/v1/cinemas**', route => route.fulfill({ json: [] }))
})

test('now-showing movie card opens its booking schedule directly', async ({ page }) => {
  await page.goto('/')
  const movie = movies[0]
  const card = page.getByRole('link', { name: `Đặt vé ${movie.title}`, exact: true })
  await expect(card).toHaveAttribute('href', `/showtimes?movieId=${movie.id}`)
  await card.click()
  await expect(page).toHaveURL(new RegExp(`/showtimes\\?movieId=${movie.id}$`))
  await expect(page.getByRole('heading', { name: movie.title, exact: true })).toBeVisible()
})

test('upcoming movie card still opens its details', async ({ page }) => {
  await page.goto('/')
  const movie = movies.find(item => item.status === 'COMING_SOON')!
  const card = page.getByRole('link', { name: `Xem chi tiết ${movie.title}`, exact: true })
  await expect(card).toHaveAttribute('href', `/movies/${movie.id}`)
  await card.click()
  await expect(page).toHaveURL(new RegExp(`/movies/${movie.id}$`))
  await expect(page.getByRole('heading', { name: movie.title, exact: true })).toBeVisible()
})
