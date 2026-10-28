import { HttpApiError } from "../../shared/apiClient";
import { authService } from "../auth/api";
import { tokenStorage } from "../auth/tokenStorage";

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export async function getCurrentUserCredentials(): Promise<{
  userId: string;
  accessToken: string;
}> {
  const user = await authService.me();

  if (!user) {
    throw new HttpApiError(
      "AUTHENTICATION_REQUIRED",
      "Vui lòng đăng nhập để tiếp tục.",
      "",
      401,
    );
  }

  if (!UUID_PATTERN.test(user.id)) {
    throw new HttpApiError(
      "INVALID_AUTH_RESPONSE",
      "Dịch vụ xác thực trả về thông tin người dùng không hợp lệ.",
      "",
      502,
    );
  }

  const tokens = tokenStorage.get();
  if (!tokens?.accessToken) {
    throw new HttpApiError(
      "AUTHENTICATION_REQUIRED",
      "Vui lòng đăng nhập để tiếp tục.",
      "",
      401,
    );
  }

  return { userId: user.id, accessToken: tokens.accessToken };
}
