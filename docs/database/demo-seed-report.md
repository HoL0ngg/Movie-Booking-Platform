# HCMC demo data — 2026-10-30

Target: Supabase project `frixuofjqvycdvnzzfcl` (Movie-Booking-Platform).
Existing records were preserved. No DDL, role, RLS, booking or payment changes were applied.

## Inserted
- `movie`: 32 movies, 3 genres, 55 movie/genre links; total catalog now 40 movies.
- `cinema_cinema`: 10 fictional Cinemate Demo cinemas in HCMC, 30 rooms, 3,000 seats.
- Every new room: rows A–J, seats 1–10; E–G VIP, other rows STANDARD (70 STANDARD / 30 VIP). No COUPLE.
- 840 published showtimes, 120 per day from 2026-10-31 through 2026-11-06, Asia/Ho_Chi_Minh.
- Starts: 09:00, 12:00, 15:00, 19:00. Prices: 75,000 / 95,000 / 95,000 / 110,000 VND; weekends add 10,000 VND.
- Prices apply to every seat of a showtime, matching current backend behavior; VIP has no separate surcharge.
- 840 ShowtimePublished outbox records committed atomically with the showtimes.

## Files and replay
- [Movie metadata and per-film sources](demo-movies.json)
- [Movie seed](seed-demo-movies.sql)
- [Cinema, room and physical-seat seed](seed-demo-cinemas.sql)
- [Showtime and outbox seed](seed-demo-showtimes.sql)
- [Read-only showtime verification](verify-demo-showtimes.sql)

These are insertion scripts for the existing schemas, not schema initialization or migrations.
The showtime script contains a fixed date window and exported movie IDs/durations.
Do not change dates while reusing the same seed identity namespace.
Each script is one atomic statement; reruns skip existing records.
Physical layouts are created only for newly inserted cinemas/rooms, never changed after scheduling.
Movie IDs are transferred as data into the cinema seed; no cross-service SQL joins are used.

## Verification
- Each seed first ran inside BEGIN / ROLLBACK before the committed execution.
- Replaying each script inside BEGIN / ROLLBACK returned zero inserted records.
- All 32 new movies have valid runtime, release date within 2026-04-30–2026-10-30, poster URL, synopsis and genres.
- All 30 new rooms have exactly 100 seats with 70 STANDARD and 30 VIP; zero invalid layouts.
- All 840 showtimes have valid prices/currency/status/sales cutoff.
- Zero overlaps with other published showtimes; minimum gap is 36 minutes.
- Every new outbox payload has 100 seats and the correct showtime identity/event type.
- Reviewed against repository code-review guidance: owner separation, atomic outbox, no existing seat ownership changes, replay safety.

## Pending runtime verification
At last check, all 840 outbox events were unpublished. Docker engine was not available on this machine.
Start Kafka, cinema-service (outbox relay), and booking-service (consumer) to materialize inventory.
Do not directly populate booking tables or mark outbox events published to bypass this process.
End-to-end booking readiness has not been confirmed.

## Content and frontend limits
Movie metadata is sourced from the linked VNPAY film pages, including theatrical re-releases within the chosen window.
Poster URLs are external and their continued availability is not guaranteed.
The schema has no trailer field; no trailer field was added.
Cinema names, addresses, rooms, seating and showtimes are explicitly demo data.
The date picker and query window now cover today plus seven days using Asia/Ho_Chi_Minh.
Further frontend test work was stopped at the user's request; no new frontend test-pass claim is made.

