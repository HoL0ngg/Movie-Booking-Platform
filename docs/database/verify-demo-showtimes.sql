WITH demo AS (
 SELECT s.* FROM cinema_cinema.showtimes s JOIN cinema_cinema.outbox_events e ON e.aggregate_id=s.id WHERE e.trace_id='demo-hcm-20261031'
), gaps AS (SELECT starts_at-lag(ends_at) OVER(PARTITION BY auditorium_id ORDER BY starts_at) AS gap FROM demo)
SELECT
 (SELECT count(*) FROM demo) showtimes,
 (SELECT count(DISTINCT auditorium_id) FROM demo) rooms,
 (SELECT count(DISTINCT movie_id) FROM demo) scheduled_movies,
 (SELECT min(gap) FROM gaps) minimum_gap,
 (SELECT count(*) FROM demo WHERE price_minor NOT BETWEEN 75000 AND 120000 OR currency<>'VND' OR sales_close_at<>starts_at-interval '15 minutes' OR status<>'PUBLISHED') invalid_showtimes,
 (SELECT count(*) FROM demo d JOIN cinema_cinema.showtimes s ON s.auditorium_id=d.auditorium_id AND s.id<>d.id AND s.status='PUBLISHED' AND tstzrange(s.starts_at,s.ends_at,'[)') && tstzrange(d.starts_at,d.ends_at,'[)')) overlap_count,
 (SELECT count(*) FROM cinema_cinema.outbox_events WHERE trace_id='demo-hcm-20261031') events,
 (SELECT count(*) FROM cinema_cinema.outbox_events WHERE trace_id='demo-hcm-20261031' AND published_at IS NOT NULL) published_events,
 (SELECT count(*) FROM cinema_cinema.outbox_events WHERE trace_id='demo-hcm-20261031' AND (jsonb_array_length(payload->'seats')<>100 OR payload->>'showtimeId'<>aggregate_id::text OR event_type<>'ShowtimePublished')) invalid_events;
