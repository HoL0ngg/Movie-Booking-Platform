import toast from 'react-hot-toast'

type Type = 'success' | 'error' | 'info'
export function showToast(message: string, type: Type = 'info') {
  const options = { id: `${type}:${message}` }
  if (type === 'success') return toast.success(message, options)
  if (type === 'error') return toast.error(message, options)
  return toast(message, options)
}
