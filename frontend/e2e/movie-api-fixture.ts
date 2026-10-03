import type { Page } from '@playwright/test'
import { movies } from '../src/mocks/data'

// Keep the existing prototype journey deterministic at the HTTP boundary.
export async function stubMovieApi(page: Page) {
  await page.route('**/api/v1/movies**', async route => {
    const url = new URL(route.request().url())
    const id = url.pathname.split('/movies/')[1]
    const status = url.searchParams.get('status')
    const movie = movies.find(item => item.id === id)
    await route.fulfill({ json: id ? movie : movies.filter(item => !status || item.status === status), status: id && !movie ? 404 : 200 })
  })
}
