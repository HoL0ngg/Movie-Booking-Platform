import toast from 'react-hot-toast'

type Type = 'success' | 'error' | 'info'
export function showToast(message: string, type: Type = 'info') {
  if (type === 'success') return toast.success(message)
  if (type === 'error') return toast.error(message)
  return toast(message)
}