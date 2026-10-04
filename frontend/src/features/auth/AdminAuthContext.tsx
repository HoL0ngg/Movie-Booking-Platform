import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { adminAuthApi, type AdminProfile, type AdminToken } from './api'

const profileKey = ['admin-profile']
interface Session { token: string; expiresAt: number }
const AdminAuthContext = createContext<{
  session: Session | null
  establish: (token: AdminToken, profile: AdminProfile, expiresAt: number) => void
  logout: () => void
} | null>(null)

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  const client = useQueryClient()
  const logout = () => {
    setSession(null)
    client.removeQueries({ queryKey: profileKey })
    client.removeQueries({ queryKey: ['admin-demo'] })
  }
  useEffect(() => {
    if (!session) return
    const timer = window.setTimeout(() => {
      setSession(null)
      client.removeQueries({ queryKey: profileKey })
      client.removeQueries({ queryKey: ['admin-demo'] })
    }, Math.max(0, session.expiresAt - Date.now()))
    return () => window.clearTimeout(timer)
  }, [session, client])
  return <AdminAuthContext.Provider value={{ session, logout, establish: (token, profile, expiresAt) => {
    client.setQueryData(profileKey, profile)
    setSession({ token: token.accessToken, expiresAt })
  } }}>{children}</AdminAuthContext.Provider>
}

// Session credentials remain in memory; a page reload requires another login.
export function useAdminAuth() {
  const context = useContext(AdminAuthContext)
  if (!context) throw new Error('AdminAuthProvider is required')
  return context
}

export function useAdminProfile() {
  const { session } = useAdminAuth()
  return useQuery({ queryKey: profileKey, queryFn: () => adminAuthApi.profile(session!.token), enabled: !!session, retry: false })
}

export function RequireAdmin() {
  const { session } = useAdminAuth()
  const profile = useAdminProfile()
  const location = useLocation()
  if (!session || profile.isError) {
    return <Navigate to="/admin/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (profile.isPending) return <div className="admin-shell admin-empty" role="status">Đang xác thực tài khoản…</div>
  return <Outlet />
}
