// Browser-storage adapter check; no live API or database is used.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')

const key = 'cinemat.admin-demo.v1'
const storage = new Map([[key, JSON.stringify({ movies: [], cinemas: [{ id: 'cinema-1' }], auditoriums: [], showtimes: [], bookings: [], payments: [] })]])
class MockApiError extends Error { constructor(code, message) { super(message); this.code = code } }
const moduleValue = { exports: {} }
const source = fs.readFileSync(path.join(__dirname, '../src/features/admin/adminService.ts'), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
vm.runInNewContext(compiled, {
  module: moduleValue, exports: moduleValue.exports, crypto: webcrypto,
  localStorage: { getItem: name => storage.get(name) ?? null, setItem: (name, value) => storage.set(name, value) },
  require: name => {
    if (name === '../../shared/apiClient') return { MockApiError, apiClient: { request: async handler => handler() } }
    if (name === '../../shared/mocks/data') return {}
    throw new Error(`Unexpected dependency: ${name}`)
  },
})
const { adminService, changeSeatType } = moduleValue.exports

async function check() {
  assert.equal((await adminService.list()).seats.length, 0, 'Old browser data remains readable')
  const room = { id: 'room-1', cinemaId: 'cinema-1', name: ' Phòng 02 ' }
  await adminService.saveRoom(room)
  assert.equal(JSON.parse(storage.get(key)).auditoriums[0].name, 'Phòng 02')
  await assert.rejects(adminService.saveRoom({ ...room, id: 'room-2' }), { code: 'ROOM_NAME_EXISTS' })
  await assert.rejects(adminService.saveRoom({ ...room, cinemaId: 'other' }), { code: 'INVALID_ROOM' })
  const seats = [{ id: 'seat-1', auditoriumId: room.id, rowLabel: 'a', seatNumber: 1, seatType: 'VIP', isActive: true }]
  await adminService.saveSeats({ auditoriumId: room.id, seats })
  assert.equal(JSON.parse(storage.get(key)).seats[0].rowLabel, 'A')
  const saved = storage.get(key)
  await assert.rejects(adminService.saveSeats({ auditoriumId: room.id, seats: [...seats, { ...seats[0], id: 'seat-2' }] }), { code: 'INVALID_SEAT' })
  assert.equal(storage.get(key), saved, 'Rejected position collision must preserve saved layout')
  await assert.rejects(adminService.saveSeats({ auditoriumId: room.id, seats: [...seats, { ...seats[0], seatNumber: 2 }] }), { code: 'INVALID_SEAT' })
  await assert.rejects(adminService.saveSeats({ auditoriumId: room.id, seats: [{ ...seats[0], seatNumber: 0 }] }), { code: 'INVALID_SEAT' })
  await assert.rejects(adminService.saveSeats({ auditoriumId: room.id, seats: [{ ...seats[0], auditoriumId: 'other' }] }), { code: 'INVALID_SEAT' })
  const row = [...seats, { ...seats[0], id: 'seat-2', seatNumber: 2 }, { ...seats[0], id: 'seat-3', seatNumber: 3 }]
  const vip = changeSeatType(row, ['seat-1', 'seat-2', 'seat-3'], 'VIP')
  assert.ok(vip.every(seat => seat.seatType === 'VIP'), 'Batch VIP applies to all selected seats')
  assert.throws(() => changeSeatType(row, ['seat-1'], 'COUPLE'), { code: 'INVALID_COUPLE' })
  assert.throws(() => changeSeatType(row, ['seat-1', 'seat-2', 'seat-3'], 'COUPLE'), { code: 'INVALID_COUPLE' })
  assert.throws(() => changeSeatType(row, ['seat-1', 'seat-3'], 'COUPLE'), { code: 'INVALID_COUPLE' })
  assert.throws(() => changeSeatType([row[0], { ...row[1], rowLabel: 'B' }], ['seat-1', 'seat-2'], 'COUPLE'), { code: 'INVALID_COUPLE' })
  assert.throws(() => changeSeatType([row[0], { ...row[1], isActive: false }], ['seat-1', 'seat-2'], 'COUPLE'), { code: 'INVALID_COUPLE' })
  const paired = changeSeatType(row, ['seat-1', 'seat-2'], 'COUPLE')
  assert.equal(paired[0].pairedSeatId, 'seat-2')
  assert.equal(paired[1].pairedSeatId, 'seat-1')
  assert.throws(() => changeSeatType(paired, ['seat-1'], 'VIP'), { code: 'INCOMPLETE_PAIR' })
  await adminService.saveSeats({ auditoriumId: room.id, seats: paired })
  const pairSaved = storage.get(key)
  await assert.rejects(adminService.saveSeats({ auditoriumId: room.id, seats: paired.filter(seat => seat.id !== 'seat-2') }), { code: 'INVALID_COUPLE' })
  assert.equal(storage.get(key), pairSaved, 'An incomplete couple cannot replace the saved layout')
  await assert.rejects(adminService.saveSeats({ auditoriumId: room.id, seats: [{ ...row[0], seatType: 'COUPLE' }] }), { code: 'INVALID_COUPLE' })
  const unpaired = changeSeatType(paired, ['seat-1', 'seat-2'], 'VIP')
  assert.equal(unpaired[0].pairedSeatId, undefined)
  assert.equal(unpaired[1].pairedSeatId, undefined)
  await adminService.saveSeats({ auditoriumId: room.id, seats: unpaired })
  const data = JSON.parse(saved)
  data.showtimes.push({ id: 'showtime-1', auditoriumId: room.id })
  storage.set(key, JSON.stringify(data))
  const scheduled = storage.get(key)
  await assert.rejects(adminService.saveSeats({ auditoriumId: room.id, seats }), { code: 'ROOM_HAS_SHOWTIMES' })
  assert.equal(storage.get(key), scheduled, 'Scheduled room layout must remain unchanged')
  await adminService.saveRoom({ ...room, name: 'Phòng IMAX' })
  assert.equal(JSON.parse(storage.get(key)).seats[0].id, 'seat-1', 'Renaming a room preserves its seats')
  console.log('Admin room/layout, batch VIP and couple checks passed')
}
check().catch(error => { console.error(error); process.exitCode = 1 })
