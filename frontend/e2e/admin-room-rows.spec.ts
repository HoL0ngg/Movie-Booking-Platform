import { expect, test, type Page } from '@playwright/test'
import { mockAdminAuth, signInAdmin } from './admin-auth-fixture'
import { auditoriums, cinemas } from '../src/shared/mocks/data'

async function openLayout(page: Page, locked = false) {
  await mockAdminAuth(page)
  await page.addInitScript(locked => {
    if (localStorage.getItem('cinemat.admin-demo.v1')) return
    const seats = ['A', 'B'].flatMap(rowLabel => [1, 2, 3].map(seatNumber => ({
      id: `${rowLabel}${seatNumber}`, auditoriumId: 'room-test', rowLabel, seatNumber,
      seatType: rowLabel === 'A' && seatNumber < 3 ? 'COUPLE' : 'STANDARD', isActive: true,
      ...(rowLabel === 'A' && seatNumber < 3 ? { pairedSeatId: seatNumber === 1 ? 'A2' : 'A1' } : {}),
    })))
    localStorage.setItem('cinemat.admin-demo.v1', JSON.stringify({
      cinemas: [{ id: 'cinema-test', name: 'Rạp thử nghiệm', city: 'Hồ Chí Minh', address: 'Địa chỉ thử nghiệm' }],
      auditoriums: [{ id: 'room-test', cinemaId: 'cinema-test', name: 'Phòng thử nghiệm' }], seats,
      movies: [], bookings: [], payments: [], showtimes: locked ? [{ auditoriumId: 'room-test' }] : [],
    }))
  }, locked)
  await page.goto('/admin/cinemas', { waitUntil: 'domcontentloaded' })
  await signInAdmin(page)
  await page.getByRole('button', { name: 'Quản lý phòng Rạp thử nghiệm', exact: true }).click()
  await page.getByRole('button', { name: 'Chỉnh sơ đồ ghế Phòng thử nghiệm', exact: true }).click()
}

test('selects whole rows, toggles them independently and saves VIP seats', async ({ page }) => {
  await openLayout(page)
  const rowA = page.getByRole('button', { name: 'Chọn cả hàng A', exact: true })
  const rowB = page.getByRole('button', { name: 'Chọn cả hàng B', exact: true })
  await page.getByRole('button', { name: /^A1, ghế Đôi,/ }).click()
  await expect(rowA).toHaveAttribute('aria-pressed', 'false')
  await rowA.click()
  await expect(page.getByRole('dialog').getByRole('status')).toContainText('Đã chọn 3 ghế: A1, A2, A3')
  await rowB.focus()
  await page.keyboard.press('Space')
  await expect(page.getByRole('dialog').getByRole('status')).toContainText('Đã chọn 6 ghế:')
  await rowA.click()
  await expect(rowA).toHaveAttribute('aria-pressed', 'false')
  await expect(rowB).toHaveAttribute('aria-pressed', 'true')
  await expect(page.getByRole('dialog').getByRole('status')).toContainText('Đã chọn 3 ghế: B1, B2, B3')
  await rowA.click()
  await page.getByRole('button', { name: 'Đổi sang VIP', exact: true }).click()
  await expect(page.getByRole('button', { name: /^[AB][123], ghế VIP,/ })).toHaveCount(6)
  await expect(rowA).toHaveAttribute('aria-pressed', 'false')
  await page.getByRole('button', { name: 'Lưu sơ đồ', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Quản lý phòng · Rạp thử nghiệm', exact: true })).toBeVisible()
  const saved = await page.evaluate(() => JSON.parse(localStorage.getItem('cinemat.admin-demo.v1')!).seats)
  expect(saved).toHaveLength(6)
  expect(saved.every((seat: { seatType: string; pairedSeatId?: string }) => seat.seatType === 'VIP' && !seat.pairedSeatId)).toBe(true)
  await page.reload({ waitUntil: 'domcontentloaded' })
  await signInAdmin(page)
  await page.getByRole('button', { name: 'Quản lý phòng Rạp thử nghiệm', exact: true }).click()
  await page.getByRole('button', { name: 'Chỉnh sơ đồ ghế Phòng thử nghiệm', exact: true }).click()
  await expect(page.getByRole('button', { name: /^[AB][123], ghế VIP,/ })).toHaveCount(6)
})

test('rooms with showtimes keep row actions and layout saving disabled', async ({ page }) => {
  await openLayout(page, true)
  await expect(page.getByRole('dialog').getByRole('status').filter({ hasText: 'Chỉ xem · Phòng đã có lịch chiếu' })).toBeVisible()
  await expect(page.getByRole('button', { name: /^[AB][123], ghế/ })).toHaveCount(6)
  for (const seat of await page.getByRole('button', { name: /^[AB][123], ghế/ }).all()) await expect(seat).toBeDisabled()
  await expect(page.getByLabel('Số hàng', { exact: true })).toBeDisabled()
  await expect(page.getByLabel('Ghế mỗi hàng', { exact: true })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Chọn cả hàng A', exact: true })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Đổi sang VIP', exact: true })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Khóa tạm thời', exact: true })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Mở khóa', exact: true })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Lưu sơ đồ', exact: true })).toBeDisabled()
  await expect(page.getByText('Ghế đôi: chọn đúng', { exact: false })).toHaveCount(0)
  await expect(page.getByText('Bấm chữ cái đầu hàng', { exact: false })).toHaveCount(0)
})

test('locks and reopens multiple seats without changing other seats or splitting couples', async ({ page }) => {
  await openLayout(page)
  await expect(page.getByText('Dữ liệu demo · Lưu trên trình duyệt', { exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Khóa tạm thời', exact: true })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Mở khóa', exact: true })).toBeDisabled()
  await page.getByRole('button', { name: /^B1, ghế Thường,/ }).click()
  await expect(page.getByRole('checkbox', { name: 'Sử dụng ghế này', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: /^A1, ghế Đôi,/ }).click()
  await page.getByRole('button', { name: /^B2, ghế Thường,/ }).click()
  await page.getByRole('button', { name: 'Khóa tạm thời', exact: true }).click()
  await expect(page.getByRole('button', { name: /, tạm khóa$/ })).toHaveCount(4)
  await expect(page.getByRole('button', { name: /, sử dụng$/ })).toHaveCount(2)
  await expect(page.getByRole('button', { name: 'Khóa tạm thời', exact: true })).toBeDisabled()
  await page.getByRole('button', { name: 'Lưu sơ đồ', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Chỉnh sơ đồ ghế Phòng thử nghiệm', exact: true })).toBeVisible()
  const saved = await page.evaluate(() => JSON.parse(localStorage.getItem('cinemat.admin-demo.v1')!).seats)
  expect(saved.filter((seat: { isActive: boolean }) => !seat.isActive)).toHaveLength(4)
  expect(saved.find((seat: { id: string }) => seat.id === 'A1').pairedSeatId).toBe('A2')
  await page.getByRole('button', { name: 'Chỉnh sơ đồ ghế Phòng thử nghiệm', exact: true }).click()
  await expect(page.getByRole('button', { name: /, tạm khóa$/ })).toHaveCount(4)
  await page.getByRole('button', { name: /^A2, ghế Đôi,/ }).click()
  await page.getByRole('button', { name: 'Chọn cả hàng B', exact: true }).click()
  await page.getByRole('button', { name: 'Mở khóa', exact: true }).click()
  await expect(page.getByRole('button', { name: /, tạm khóa$/ })).toHaveCount(0)
  await expect(page.getByRole('button', { name: /, sử dụng$/ })).toHaveCount(6)
  await page.getByRole('button', { name: 'Lưu sơ đồ', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Chỉnh sơ đồ ghế Phòng thử nghiệm', exact: true })).toBeVisible()
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem('cinemat.admin-demo.v1')!).seats.every((seat: { isActive: boolean }) => seat.isActive))).toBe(true)
})

for (const legacy of [false, true]) {
  test(`existing demo rooms display their read-only layout with ${legacy ? 'older' : 'fresh'} browser data`, async ({ page }) => {
    await mockAdminAuth(page)
    if (legacy) await page.addInitScript(({ cinemas, auditoriums }) => {
      localStorage.setItem('cinemat.admin-demo.v1', JSON.stringify({
        cinemas, auditoriums, movies: [], bookings: [], payments: [], seats: [],
        showtimes: [{ auditoriumId: auditoriums[0].id }],
      }))
    }, { cinemas, auditoriums })
    await page.goto('/admin/cinemas', { waitUntil: 'domcontentloaded' })
    await signInAdmin(page)
    await page.getByRole('button', { name: `Quản lý phòng ${cinemas[0].name}`, exact: true }).click()
    await page.getByRole('button', { name: `Chỉnh sơ đồ ghế ${auditoriums[0].name}`, exact: true }).click()
    await expect(page.getByRole('button', { name: /^[A-F]\d+, ghế/ })).toHaveCount(60)
    await expect(page.getByRole('button', { name: 'A1, ghế Thường, sử dụng', exact: true })).toBeDisabled()
    await expect(page.getByRole('button', { name: 'E1, ghế VIP, sử dụng', exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Lưu sơ đồ', exact: true })).toBeDisabled()
  })
}
