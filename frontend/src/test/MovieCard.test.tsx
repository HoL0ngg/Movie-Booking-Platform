import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { MovieCard } from '../components/MovieCard'
import { movies } from '../mocks/data'
import { expect, it } from 'vitest'

it('links a movie card to its details with an accessible label', () => {
  render(<MemoryRouter><MovieCard movie={movies[0]} /></MemoryRouter>)
  expect(screen.getByRole('link', { name: `Xem chi tiết ${movies[0].title}` })).toHaveAttribute('href', `/movies/${movies[0].id}`)
})
