import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useCurrentUser } from './hooks'

export function RequireAuth() {
  const { data: user, isLoading } = useCurrentUser()
  const loc = useLocation()
  if (isLoading) return null
  return user ? <Outlet /> : <Navigate to="/login" state={{ from: loc }} replace />
}