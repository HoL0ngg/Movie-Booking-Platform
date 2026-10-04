import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authService } from './api'
import { showToast } from '../../shared/toast'
import { authErrorMessage } from './authErrors'

export const useCurrentUser = () => useQuery({
  queryKey: ['current-user'],
  queryFn: () => authService.me(),
  retry: false, staleTime: 5 * 60_000,
})

export const useAuth = () => {
  const client = useQueryClient()
  const loadUser = async () => client.setQueryData(['current-user'], await authService.me())
  return {
    login: useMutation({
      mutationFn: ({ email, password }: { email: string; password: string }) => authService.login(email, password),
      onSuccess: async () => { await loadUser(); showToast('Đăng nhập thành công', 'success') },
      onError: (e: Error) => showToast(authErrorMessage(e), 'error'),
    }),
    register: useMutation({
      mutationFn: ({ email, password }: { email: string; password: string }) => authService.register(email, password),
      onSuccess: async () => { await loadUser(); showToast('Tạo tài khoản thành công', 'success') },
      onError: (e: Error) => showToast(authErrorMessage(e), 'error'),
    }),
    logout: useMutation({
      mutationFn: authService.logout,
      onSettled: () => {
        client.setQueryData(['current-user'], null)
        client.removeQueries({ queryKey: ['bookings'] })
        showToast('Đã đăng xuất', 'success')
      },
    }),
  }
}