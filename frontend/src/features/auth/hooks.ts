import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authService } from './api'

export const useCurrentUser = () => useQuery({ queryKey: ['current-user'], queryFn: async () => authService.current() })

export const useAuth = () => {
  const client = useQueryClient()
  const refresh = () => client.invalidateQueries({ queryKey: ['current-user'] })
  return {
    login: useMutation({ mutationFn: ({ email, password }: { email: string; password: string }) => authService.login(email, password), onSuccess: refresh }),
    register: useMutation({ mutationFn: ({ name, email, password }: { name: string; email: string; password: string }) => authService.register(name, email, password), onSuccess: refresh }),
    logout: useMutation({ mutationFn: authService.logout, onSuccess: refresh }),
  }
}

