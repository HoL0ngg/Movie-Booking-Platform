---
name: threejs
description: Add or review optional Three.js/React Three Fiber visuals for the cinema web experience without coupling WebGL to core booking behavior.
---

# Three.js

## When to use

Use this skill only when a requested cinema visualization materially benefits from 3D, such as an auditorium preview. For ordinary layout, controls, animation, or a seat-selection grid, prefer semantic HTML and CSS. Read `AGENTS.md` and the frontend skill before changing booking-facing visuals.

## Architectural rules

- WebGL is progressive enhancement; authentication, browsing, seat selection, checkout, payment, and ticket access must remain functional without it.
- PostgreSQL/backend APIs remain authoritative. A 3D object's appearance or local interaction state never grants a hold or confirms a booking.
- Maintain a semantic DOM representation/fallback with the same essential information and actions.
- Respect accessibility, device capability, performance budgets, and `prefers-reduced-motion` above visual effect.

## Implementation rules

- Lazy-load React Three Fiber/Three.js modules behind a stable fallback and error boundary.
- Keep render-loop work bounded; reuse geometry/materials, dispose GPU resources, limit textures/lights/post-processing, and pause or reduce work when hidden.
- Drive visuals from typed application state without duplicating server authority. Send reservation commands through the normal frontend API layer with stable idempotency keys.
- Provide keyboard-accessible DOM controls and descriptive labels for seats; synchronize focus/selection indicators between DOM and 3D views.
- Disable or simplify continuous animation for reduced motion and low-capability devices. Test context loss and module-load failure.
- Measure bundle, frame-time, and memory impact before accepting a complex scene.

## Forbidden patterns

- Canvas-only navigation or seat selection.
- Core booking state stored only in scene objects, raycast results, or animation state.
- Per-frame React state updates, unbounded particle/effect loops, leaked geometry/materials/textures, or eager loading on unrelated routes.
- 3D added where CSS/SVG/DOM communicates the information as well or better.
- Motion that ignores reduced-motion preferences or obscures focus/status.

## Testing requirements

- Test the full relevant journey with WebGL disabled, initialization failed, and the visual module still loading.
- Test keyboard and screen-reader-equivalent DOM interactions, reduced-motion behavior, resize/responsiveness, and context cleanup.
- Add component tests for state synchronization and at least a targeted browser test for the progressive-enhancement boundary.
- Record performance evidence for material scene changes; avoid snapshot-only confidence.

## Review checklist

- [ ] The feature genuinely benefits from 3D and has a complete DOM fallback.
- [ ] Booking/payment correctness is independent of canvas state.
- [ ] Keyboard, focus, labels, contrast, and reduced motion are supported.
- [ ] Loading, failure, context loss, and cleanup paths are safe.
- [ ] Scene resources and render-loop work are bounded.
- [ ] Heavy code is route/component lazy-loaded.
- [ ] Tests prove the fallback and state synchronization.

