import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { showToast } from '../../shared/toast'
import { authErrorMessage } from './authErrors'
import { authService } from './api'


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
    requestOtp: useMutation({
      mutationFn: ({ email, password }: { email: string; password: string }) => authService.requestOtp(email, password),
      onSuccess: () => showToast('Đã gửi mã OTP tới email của bạn', 'success'),
      onError: (e: Error) => showToast(authErrorMessage(e), 'error'),
    }),
    verifyOtp: useMutation({
      mutationFn: ({ email, otp }: { email: string; otp: string }) => authService.verifyOtp(email, otp),
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