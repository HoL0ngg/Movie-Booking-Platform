import type { Page } from '@playwright/test'

export async function mockAdminAuth(page: Page, roles = ['ADMIN'], expiresIn = 900) {
  await page.route('**/api/v1/auth/login', route => route.fulfill({ json: { accessToken: 'test-only-admin-token', tokenType: 'Bearer', expiresIn } }))
  await page.route('**/api/v1/me', route => route.request().headers().authorization === 'Bearer test-only-admin-token'
    ? route.fulfill({ json: { id: 'admin-test-id', email: 'admin@example.test', roles } })
    : route.fulfill({ status: 401, json: { code: 'TOKEN_INVALID' } }))
}

export async function signInAdmin(page: Page) {
  await page.getByLabel('Email công việc').fill('admin@example.test')
  await page.getByLabel('Mật khẩu', { exact: true }).fill('test-only-password')
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
}
