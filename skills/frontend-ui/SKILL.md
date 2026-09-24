---
name: frontend-ui
description: Build or review the React web UI, accessible booking journeys, server-state handling, routing, forms, and resilient API interactions.
---

# Frontend UI

## When to use

Use this skill for `frontend/web`, React/TypeScript components, routes, TanStack Query, booking/payment screens, forms, responsive behavior, or accessibility. Read `AGENTS.md` and `docs/api-contracts.md` before wiring server interactions.

## Architectural rules

- Organize product code by feature under `src/features`: `auth`, `movies`, `cinemas`, `showtimes`, `booking`, `payment`, and `profile`.
- Keep server state in TanStack Query. Local UI state may control presentation, but must not become a second authority for availability, reservations, or payments.
- All booking/payment outcomes come from backend APIs or verified backend state. A redirect from a payment provider is never proof of success.
- Three.js/React Three Fiber is optional progressive enhancement. The complete booking journey must work without WebGL.
- Preserve backend authorization boundaries; hiding a control is not authorization.

## Implementation rules

- Use typed API boundaries and map standard errors by stable `code`, retaining `traceId` for support.
- Generate one reservation/payment idempotency key per user intent and reuse it for transport retries; create a new key only for a genuinely new action.
- After a reservation conflict, refetch authoritative seat state and explain the conflict without silently changing the requested seat set.
- Implement semantic HTML, keyboard navigation, visible focus, sufficient contrast, appropriate labels, and screen-reader announcements for meaningful async changes.
- Each seat exposes a label such as "A5, VIP seat, available" and state via text/icon/semantics, not color alone.
- Support loading, empty, error, offline/retry, conflict, expired, pending, success, and cancellation states without optimistic claims of booking/payment success.
- Respect `prefers-reduced-motion`, lazy-load heavy visuals, and keep core content usable during slow networks or visual-module failure.

## Forbidden patterns

- Treating cached/client state, a WebSocket message, or a selected-seat highlight as authoritative ownership.
- Confirming payment from redirect parameters or rendering a ticket before backend confirmation.
- Duplicating broad server responses into global stores without a demonstrated UI-only need.
- Retrying a mutation with a newly generated idempotency key.
- Click-only controls, color-only seat status, hidden focus, inaccessible canvas-only seat maps, or motion without a reduced-motion mode.
- Domain pricing, availability, authorization, or payment logic in frontend code.

## Testing requirements

- Add component tests for meaningful state transitions, accessible names/roles, keyboard selection, conflict messaging, and retry behavior.
- Add Playwright coverage for the critical journey: movie -> cinema -> showtime -> seats -> reservation -> checkout -> payment -> ticket.
- Include backend-declared seat conflict, hold expiry, pending payment, failed payment, duplicate submission, refresh/recovery, and WebGL-unavailable scenarios.
- Run automated accessibility checks and manually verify keyboard/focus behavior for the seat map and checkout.

## Review checklist

- [ ] Server state has one clear TanStack Query source and invalidation strategy.
- [ ] Mutation retries preserve the original idempotency key.
- [ ] UI never asserts reservation/payment success before backend confirmation.
- [ ] Every interactive control is keyboard operable with visible focus.
- [ ] Seat states are understandable without color and have accessible labels.
- [ ] Loading, errors, conflicts, expiry, and pending states are handled.
- [ ] Heavy visuals are lazy and the non-WebGL fallback is complete.
- [ ] Tests cover the changed critical user behavior.

