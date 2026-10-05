import { HttpApiError } from '../../shared/apiClient'

const byCode: Record<string, string> = {
  INVALID_CREDENTIALS: 'Email hoặc mật khẩu không đúng.',
  EMAIL_ALREADY_REGISTERED: 'Email này đã được đăng ký.',
  VALIDATION_ERROR: 'Thông tin không hợp lệ. Vui lòng kiểm tra lại.',
  AUTHENTICATION_REQUIRED: 'Vui lòng đăng nhập để tiếp tục.',
  TOKEN_INVALID: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  USER_NOT_FOUND: 'Không tìm thấy tài khoản.',
  FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
  OTP_INVALID: 'Mã OTP không đúng.',
  OTP_EXPIRED: 'Mã OTP đã hết hạn. Vui lòng gửi lại mã.',
  OTP_TOO_MANY_ATTEMPTS: 'Nhập sai quá nhiều lần. Vui lòng gửi lại mã mới.',
  OTP_RESEND_TOO_SOON: 'Vui lòng chờ một chút trước khi gửi lại mã.',
  EMAIL_SEND_FAILED: 'Không gửi được email. Vui lòng thử lại.',
  OTP_REQUIRED: 'Vui lòng đăng nhập bằng xác thực email.',
  OTP_STORE_UNAVAILABLE: 'Hệ thống xác thực đang gián đoạn. Vui lòng thử lại sau.',
  INTERNAL_ERROR: 'Máy chủ gặp lỗi. Vui lòng thử lại.',
}

const byStatus: Record<number, string> = {
  429: 'Bạn thao tác quá nhiều lần. Vui lòng thử lại sau.',
  502: 'Máy chủ tạm thời không phản hồi. Vui lòng thử lại.',
  503: 'Dịch vụ đang bảo trì. Vui lòng thử lại sau.',
  504: 'Máy chủ phản hồi quá lâu. Vui lòng thử lại.',
}

export function authErrorMessage(error: unknown): string {
  if (error instanceof HttpApiError) {
    return byCode[error.code] ?? byStatus[error.status] ?? 'Đã có lỗi xảy ra. Vui lòng thử lại.'
  }
  return 'Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại.'
}