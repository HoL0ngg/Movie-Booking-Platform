# Free movie catalog deployment

Scope: the published movie catalog and movie details only. Auth, showtimes,
reservations, payments and AI require their own deployments and are not wired here.
Service ownership and JWT authorization remain unchanged.

## Render

Existing service: `movie-booking-movie-service`, Singapore, Free instance.
Repository root is the Docker context; Dockerfile is `Dockerfile.render-movie`.
The build targets the child POM directly because the image contains only that
module and its parent POM. Runtime uses Render's `PORT` (fallback 10000).
Set HTTP health check to `/actuator/health`.

Runtime variables (secret values must stay outside Git):

- `DB_URL`: JDBC URL from the existing Supabase **Session pooler**, port 5432,
  with `?sslmode=require`. Do not purchase the IPv4 add-on.
- `DB_USER`: the isolated movie role followed by `.<project-ref>` for Supavisor.
- `DB_PASSWORD`: that role's password, not the project administrator password.
- `AUTH_JWT_SECRET`: the existing auth-service signing secret, at least 32 bytes.

Hibernate keeps `ddl-auto=validate`; SQL initialization stays disabled. Do not
replay `db/schema.sql` on the hosted database. The existing project uses schema
`movie`; entities explicitly name that schema.

Before provisioning credentials, inspect role membership and permissions. Grant
the application only USAGE on `movie`, SELECT on its three catalog tables,
INSERT/UPDATE on `movie.movies`, and INSERT/DELETE on `movie.movie_genres`.
No other service schema access is needed. Hosted role creation/privilege changes
require owner confirmation under AGENTS.md section 18.
The reviewed one-time SQL is [provision-movie-service-role.sql](database/provision-movie-service-role.sql).

## Vercel

Use `frontend` as the project Root Directory, Vite framework, `npm run build`,
output `dist`, and `VITE_API_BASE_URL=/api/v1`. Redeploy after changing build-time
variables. `API_PROXY_TARGET` is only for local Vite development.

`frontend/vercel.json` proxies the public movie paths to the existing Render
service through the website's own origin and supports React Router deep links.
Other `/api/` paths do not fall through to HTML. This temporary catalog-only
edge configuration needs no browser CORS relaxation and forwards no database
credentials to the browser. Once the gateway is deployed, replace the movie
rewrite with the gateway route and configure the other owner APIs there.

## Verification and cost limits

After Render reports `live`, verify `/actuator/health`, the movie list, a real
movie detail, filtering, an absent ID, and rejection of unauthenticated admin
access. Compare the catalog with published rows in the owning database. Then
verify the same list/detail requests via the Vercel origin and a deep link.

Use existing Free resources only: no new database, disk, paid instance, pooler
or IPv4 add-on. Free does not mean unlimited: Render's instance hours are shared
by the workspace, services sleep on inactivity, and bandwidth/build overages
can be billed when a payment method is present. Check billing limits before
claiming a guarantee of zero charges; do not add a payment method or upgrade.

References: [Render Free](https://render.com/docs/free),
[Supabase connections](https://supabase.com/docs/guides/database/connecting-to-postgres),
[Vercel rewrites](https://vercel.com/docs/rewrites).

## Inspection on 2026-11-16

- Render's initial deployment of `bbf252f` failed because Maven's root reactor
  referenced modules omitted from the Docker image. No successful deploy exists.
- Supabase project `frixuofjqvycdvnzzfcl` is healthy and its organization is Free.
  Schema `movie` has 40 published movies; no movie application role exists yet.
- Vercel website `https://movie-booking-platform-tau.vercel.app/` responds 200;
  `/api/v1/movies` responds 404 before this routing configuration is deployed.
- The corrected Maven child-module package and frontend build pass. Rewrite
  assertions cover the movie target and exclusion of API routes from SPA HTML.
- Frontend lint reports three existing errors in unchanged `GoogleSignInButton.tsx`
  and `SeatPage.tsx`. Backend tests remain skipped by repository configuration.
- Local Docker image verification is unavailable because Docker Engine is stopped.
  Hosted role creation, credential configuration, merge and live verification
  remain pending owner approval; these checks do not establish a live connection.
