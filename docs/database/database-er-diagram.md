# Database diagrams

The tables below are implemented by each service's V2 Flyway migration. [DBML](schema.dbml) lists columns and relationships; [database design](../database-design.md) explains PostgreSQL-specific checks and future application transitions. DBML's six namespaces represent six **separate databases**. The business workflows are not implemented yet.

## System-level ownership

Arrows here mean service ownership of a database, **not** foreign keys. The gateway has no database.

```mermaid
flowchart LR
    Auth[auth-service] --> AuthDB[(cinema_auth)]
    Movie[movie-service] --> MovieDB[(cinema_movie)]
    Cinema[cinema-service] --> CinemaDB[(cinema_cinema)]
    Booking[booking-service] --> BookingDB[(cinema_booking)]
    Payment[payment-service] --> PaymentDB[(cinema_payment)]
    Notification[notification-service] --> NotificationDB[(cinema_notification)]
```

## Authentication database

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : receives
    ROLES ||--o{ USER_ROLES : grants
    USERS ||--o{ REFRESH_SESSIONS : owns
    USERS {
        uuid id PK
        text email_normalized UK
        text password_hash
        varchar status
        timestamptz created_at
    }
    ROLES {
        uuid id PK
        varchar code UK
    }
    USER_ROLES {
        uuid user_id PK, FK
        uuid role_id PK, FK
        timestamptz granted_at
    }
    REFRESH_SESSIONS {
        uuid id PK
        uuid user_id FK
        bytea token_hash UK
        timestamptz expires_at
        timestamptz revoked_at
    }
```

## Movie catalog database

```mermaid
erDiagram
    MOVIES ||--o{ MOVIE_GENRES : classified_as
    GENRES ||--o{ MOVIE_GENRES : contains
    MOVIES {
        uuid id PK
        text title
        int duration_minutes
        date release_date
        varchar status
    }
    GENRES {
        uuid id PK
        text name UK
    }
    MOVIE_GENRES {
        uuid movie_id PK, FK
        uuid genre_id PK, FK
    }
```

## Cinema and showtime database

`SEATS` describes permanent auditorium positions. Live availability is held by booking, not here. `SHOWTIMES.movie_id` is a logical movie-service ID without a database FK.

```mermaid
erDiagram
    CINEMAS ||--o{ AUDITORIUMS : contains
    AUDITORIUMS ||--o{ SEATS : defines
    AUDITORIUMS ||--o{ SHOWTIMES : hosts
    CINEMAS {
        uuid id PK
        text name
        text city
        text timezone
    }
    AUDITORIUMS {
        uuid id PK
        uuid cinema_id FK
        text name
    }
    SEATS {
        uuid id PK
        uuid auditorium_id FK
        varchar row_label
        int seat_number
        varchar seat_type
        boolean is_active
    }
    SHOWTIMES {
        uuid id PK
        uuid auditorium_id FK
        uuid movie_id
        timestamptz starts_at
        timestamptz ends_at
        bigint price_minor
        varchar status
        bigint snapshot_version
    }
    OUTBOX_EVENTS {
        uuid event_id PK
        uuid aggregate_id
        varchar event_type
        timestamptz published_at
    }
```

`OUTBOX_EVENTS` is intentionally unconnected: its `aggregate_id` is an event key, not a physical FK to one table.

## Booking database

`SHOWTIME_SNAPSHOTS` and `SHOWTIME_SEATS` contain copied cinema identifiers without cross-database FKs. `SHOWTIME_SEATS` is the sole authoritative availability table. `BOOKING_ITEMS` is created only when a booking confirms; its unique `(showtime_id, seat_id)` prevents a second successful sale.

```mermaid
erDiagram
    SHOWTIME_SNAPSHOTS ||--o{ SHOWTIME_SEATS : inventories
    SHOWTIME_SNAPSHOTS ||--o{ RESERVATIONS : receives
    RESERVATIONS ||--o{ RESERVATION_SEATS : selects
    SHOWTIME_SEATS ||--o{ RESERVATION_SEATS : selected_in
    RESERVATIONS ||--o| BOOKINGS : checks_out_as
    RESERVATIONS ||--o{ SHOWTIME_SEATS : currently_holds
    BOOKINGS ||--o{ SHOWTIME_SEATS : currently_owns
    BOOKINGS ||--o{ BOOKING_ITEMS : entitles
    SHOWTIME_SEATS ||--o| BOOKING_ITEMS : sold_as
    SHOWTIME_SNAPSHOTS {
        uuid showtime_id PK
        uuid cinema_id
        uuid auditorium_id
        uuid movie_id
        varchar status
        bigint snapshot_version
    }
    SHOWTIME_SEATS {
        uuid showtime_id PK, FK
        uuid seat_id PK
        bigint price_minor
        varchar status
        uuid reservation_id FK
        uuid booking_id FK
        timestamptz hold_expires_at
        bigint version
    }
    RESERVATIONS {
        uuid id PK
        uuid showtime_id FK
        uuid user_id
        varchar status
        timestamptz hold_expires_at
    }
    RESERVATION_SEATS {
        uuid reservation_id PK, FK
        uuid showtime_id FK
        uuid seat_id PK, FK
        bigint price_minor
    }
    BOOKINGS {
        uuid id PK
        uuid reservation_id FK, UK
        uuid showtime_id FK
        uuid payment_id UK
        bigint amount_minor
        varchar status
    }
    BOOKING_ITEMS {
        uuid id PK
        uuid booking_id FK
        uuid showtime_id FK
        uuid seat_id FK
        bigint price_minor
    }
    IDEMPOTENCY_KEYS {
        uuid actor_id PK
        varchar operation PK
        varchar idempotency_key PK
        bytea request_hash
    }
    OUTBOX_EVENTS {
        uuid event_id PK
        uuid aggregate_id
        varchar event_type
        timestamptz published_at
    }
    PROCESSED_EVENTS {
        varchar consumer_name PK
        uuid event_id PK
        timestamptz processed_at
    }
```

The reservation/booking-to-seat references use composite local FKs with `showtime_id`; the Mermaid lines simplify those composite keys for readability. `IDEMPOTENCY_KEYS.actor_id`, `BOOKINGS.payment_id`, and the inbox/outbox event IDs are not physical FKs to other databases or arbitrary aggregates.

## Payment database

Booking, reservation, and user IDs are logical references. Provider transaction, event, merchant, and refund identifiers are local unique identities; no raw card data is modeled.

```mermaid
erDiagram
    PAYMENTS ||--o{ PAYMENT_ATTEMPTS : tries
    PAYMENTS ||--o{ PROVIDER_EVENTS : receives
    PAYMENTS ||--o| REFUNDS : compensates
    PAYMENTS {
        uuid id PK
        uuid booking_id UK
        uuid reservation_id
        uuid user_id
        bigint amount_minor
        varchar currency
        varchar status
    }
    PAYMENT_ATTEMPTS {
        uuid id PK
        uuid payment_id FK
        varchar provider
        varchar merchant_reference
        varchar provider_transaction_id
        varchar status
    }
    PROVIDER_EVENTS {
        uuid id PK
        uuid payment_id FK
        varchar provider
        varchar event_identity
        timestamptz received_at
    }
    REFUNDS {
        uuid id PK
        uuid payment_id FK, UK
        uuid refund_request_id UK
        varchar merchant_reference
        varchar provider_refund_id
        varchar status
    }
    OUTBOX_EVENTS {
        uuid event_id PK
        uuid aggregate_id
        varchar event_type
        timestamptz published_at
    }
    PROCESSED_EVENTS {
        varchar consumer_name PK
        uuid event_id PK
        timestamptz processed_at
    }
```

## Notification database

One notification row is both the durable event deduplication record and the delivery job. `source_event_id` and `user_id` are logical IDs, not local FKs. Its unique `(source_event_id, channel)` prevents duplicate jobs for the same delivery channel.

```mermaid
erDiagram
    NOTIFICATIONS {
        uuid id PK
        uuid source_event_id
        uuid user_id
        varchar channel
        text recipient_address
        varchar status
        int attempt_count
        timestamptz next_attempt_at
        varchar provider_message_id
    }
```

## Relationship and implementation boundary

Every ER relationship above corresponds to an implemented **local** DBML `Ref`. Cross-service IDs appear as plain attributes and will be resolved through APIs or events when those workflows are implemented. The V2 migrations are authoritative if a diagram ever differs from the database.
