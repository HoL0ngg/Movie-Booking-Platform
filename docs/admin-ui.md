# Admin UI prototype

The admin workspace lives under `/admin`, within the existing React app. It uses a separate light layout, neutral surfaces, and a restrained green accent. Routes:

| Route | Capability |
|---|---|
| `/admin` | Derived demo metrics, seven-day revenue chart, popular movies, recent payments |
| `/admin/movies` | Search/filter, add and edit movie metadata |
| `/admin/cinemas` | Search/city filter, add and edit cinema information, view demo rooms |
| `/admin/showtimes` | Search/cinema filter, pagination, add/edit demo schedules |
| `/admin/bookings` | Search/status filter, pagination, read-only booking detail |
| `/admin/payments` | Search/status filter, pagination, read-only payment detail |

## Current boundary

All records are synthetic and explicitly labelled demo. `features/admin/adminService.ts` is the transport boundary, using the existing mock request helper. TanStack Query owns the fetched snapshot; components hold only filters, pagination, and modal state. Changes persist under `cinemat.admin-demo.v1`, isolated from customer prototype state. New demo cinemas receive one default room. Movie duration changes are rejected when demo showtimes already exist; overlapping room schedules and editing showtimes with demo bookings are rejected. These checks demonstrate form feedback, not production booking/concurrency guarantees.

There are no backend admin APIs or admin authentication yet. The route intentionally exposes only the demo workspace. A frontend role check would not protect real resources. Before connecting production data, define owner-specific admin API contracts and enforce authentication/authorization in each receiving service. Do not ship this mock adapter as a production administration interface.

Payments and bookings are read-only. The prototype does not confirm payments, change booking state, cancel tickets, charge, or refund. No cardholder data or provider credentials are stored. Future payment operations belong to payment-service and must preserve server verification, idempotency, and the Saga/Outbox architecture.

## Verification

Run `npm run build` and `npm run lint` from `frontend/`. A single focused Playwright case is in `e2e/admin.spec.ts` for persisted cinema creation, filtering, read-only payment details, and mobile navigation. Run `npm run test:e2e -- e2e/admin.spec.ts` when Chromium is available. No new dependencies were added.
