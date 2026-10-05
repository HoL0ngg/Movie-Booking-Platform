import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import App from './app/App'
import { BookingDraftProvider } from './features/booking/BookingContext'
import './app/global.css'
import { AdminAuthProvider } from './features/auth/AdminAuthContext'
import { AppToaster } from './shared/AppToaster'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1 },
    mutations: { retry: false },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <BookingDraftProvider>
          <AdminAuthProvider>
            <App />
            <AppToaster />
          </AdminAuthProvider>
        </BookingDraftProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
