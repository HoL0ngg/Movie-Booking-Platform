# Additional demo catalog and schedule load investigation

## Findings

The hosted database already contained six future PUBLISHED showtimes. An empty
successful schedule response is HTTP 200 with `[]`; it does not cause the UI's
load-error message. The frontend loads eight dates with `Promise.all`, so one
failed date rejects the complete schedule instead of silently hiding missing data.

Cinema-service logs show graceful shutdown at 01:52:11 UTC and a new instance
starting at 01:57:33 UTC. Spring logged startup taking 129.599 seconds, exceeding
Vercel's 120-second external rewrite budget. The initial direct API check timed
out at 120 seconds. Once started, the same query returned HTTP 200 through the
cinema service, gateway and Vercel in approximately 0.8/0.8/1.2 seconds. No runtime
database error or OOM was found; memory remained below the 512 MiB instance limit.
This supports Free-tier cold start as the cause of the transient load failure,
not absent showtimes. No paid upgrade or continuous keep-alive was added.

## Changes

Cinema and schedule GET queries now reuse the existing bounded retry rule with
5/10-second delays. Client errors and mutations retain their prior behavior.
Loading copy explains the possible two-minute startup; errors remain distinct
from an empty schedule and keep the retry button. Failed dates are never turned
into empty successful results.

## Added data

These are explicitly fictional classroom demo movies. Each was created as
PUBLISHED through movie-service's admin API, with existing genre references and
release date 2026-11-17. The owner service persisted the records in Supabase;
no other service schema was written directly.

| Movie | ID | Duration | Vietnam time |
|---|---|---:|---|
| Hành Trình Sao Băng (Demo) | bec33b97-ebaf-4a24-af57-1624b56bf3eb | 100 min | 12:30 |
| Bí Mật Rạp Chiếu (Demo) | 938a1087-fac9-4623-a4bb-b6428f06e8a4 | 105 min | 15:00 |
| Chuyến Tàu Bình Minh (Demo) | a95ffa70-92b9-454a-bb20-d7c65ecb3c10 | 110 min | 17:30 |

Each movie has one session daily on November 17, 18 and 19 at Cinemate Bến Thành,
Phòng 2 (`e4afa950-bc90-b2b3-fa95-1901a3b7d479`), price 95,000 VND, sales closing
15 minutes before start. All nine sessions were created/published through the
cinema owner API. Its transactional outbox published to Kafka; booking-service
created the seat projection. No historical showtime or booking was altered.

## Verification and limits

- Public catalog has 43 movies; the three new details return the expected titles.
- All eight daily schedule reads for each new movie succeeded through Vercel,
  with exactly three sessions per movie.
- All nine new seat maps contain 100 AVAILABLE seats (900 total).
- No schema changes, payment operations, credentials or paid resources were added.
- Frontend build, scoped ESLint and diff checks passed. All eight focused
  showtime-filter Playwright tests passed using installed Chrome, including
  temporary 502 recovery, no retries on a 400, and successful empty responses.
  No browser runtime was downloaded. Code review found no changes to mutation,
  payment, locking, authorization or event semantics.

Free services still sleep; retries cannot promise zero cold-start failures.
Run the bounded `infrastructure/scripts/warm-render-demo.ps1` shortly before class.
For another class date, create future sessions through the owning cinema API.

References: [Render Free](https://render.com/docs/free),
[Vercel limits](https://vercel.com/docs/limits).
