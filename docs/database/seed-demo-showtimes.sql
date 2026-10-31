-- Demo 2026-10-31 to 2026-11-06. Movie IDs exported from owner, no cross-service SQL.
-- New showtimes and their immutable outbox events are atomic. Existing data is unchanged.
WITH movies(n,id,duration) AS (VALUES (0,'0829a604-3052-4840-a083-8593ff87d731'::uuid,120),
(1,'0978eefc-cc01-65d2-729f-91f763178e48'::uuid,120),
(2,'0f4550bb-06c9-5ac6-9dd2-8eb8b7dcba3a'::uuid,111),
(3,'14602db9-edd3-ed0c-afec-265d7e912bd0'::uuid,119),
(4,'181951cf-b372-ef03-102f-9622ba8c72da'::uuid,125),
(5,'1a4aebcd-e85a-0af7-334d-bbb446d10680'::uuid,126),
(6,'1c9a20dd-71ea-39c1-26aa-61b8c5a4ebcd'::uuid,117),
(7,'1e4bbc48-18ce-4c7d-21b6-4c68295a37b8'::uuid,97),
(8,'2dda8ba6-2974-8f3f-c0cc-9b1e23850cc3'::uuid,87),
(9,'2df75ebb-e082-e1c5-cc9d-82094b270ff2'::uuid,108),
(10,'30e7ca75-0866-27f1-64c6-984accf537d5'::uuid,124),
(11,'352e020d-3490-d700-2698-ab1252b2fb0d'::uuid,106),
(12,'36cec03c-7921-00b5-fea1-aa095ea4ac7e'::uuid,86),
(13,'3cc0961d-b396-d1ea-b3c2-a986440c9a09'::uuid,94),
(14,'43502865-abf9-ff3d-a125-e99b6cefb6e3'::uuid,116),
(15,'441deb28-8584-a5d0-b720-cf6c9ad962bc'::uuid,94),
(16,'468bd332-d73d-990e-9491-868b98cc46c6'::uuid,98),
(17,'496830e5-1a41-a84f-0a14-33b4e0fd554e'::uuid,101),
(18,'585e5faf-255b-4e54-41fa-3cb9141bb19d'::uuid,98),
(19,'74367a8f-be96-c7d7-bd91-25d98fca1fb9'::uuid,104),
(20,'7bd4b18a-b7e1-9f47-8ab3-30bbb2bcab85'::uuid,99),
(21,'82b0c22c-f958-fc90-3b4b-bb605d068722'::uuid,97),
(22,'8e91d272-ad5d-7dd8-6737-dec0bc17b444'::uuid,91),
(23,'92f38289-dea8-3692-2515-b648b926a0b3'::uuid,144),
(24,'9dd61655-8a9c-76b7-db5f-2bb71a140fc5'::uuid,95),
(25,'ad4eb0e2-7d53-6ed3-652d-6f5da9a87227'::uuid,127),
(26,'ae608a41-3aeb-569f-8140-cb31868d583d'::uuid,116),
(27,'be91a789-78ff-4cc5-94a6-55bb8db16232'::uuid,144),
(28,'c7b626cf-5852-be42-6c11-6829a92b616d'::uuid,109),
(29,'d2e8b601-8dc8-c943-5c0e-278bfd2dc617'::uuid,122),
(30,'e98e3171-d3e4-3026-6473-5e9673673a3b'::uuid,132),
(31,'fca17013-7cd9-7d0d-3a1e-215b60ba854a'::uuid,93)),
locked_rooms AS MATERIALIZED (
 SELECT a.id,a.cinema_id FROM cinema_cinema.auditoriums a JOIN cinema_cinema.cinemas c ON c.id=a.cinema_id
 WHERE c.id IN (SELECT md5('cinemate-demo-hcm-v1-'||n)::uuid FROM generate_series(1,10) n)
 AND a.id IN (SELECT md5(c.id::text||'-room-'||r)::uuid FROM generate_series(1,3) r)
 ORDER BY a.id FOR UPDATE OF a
), rooms AS (SELECT *,row_number() OVER (ORDER BY id)-1 AS n FROM locked_rooms),
slots(n,hour,price) AS (VALUES(0,9,75000),(1,12,95000),(2,15,95000),(3,19,110000)),
candidates AS (
 SELECT md5('cinemate-demo-showtime-v1-'||a.id||'-'||d||'-'||s.n)::uuid id,
 a.id auditorium_id,m.id movie_id,
 ((date '2026-10-31'+d)+make_time(s.hour,0,0)) AT TIME ZONE 'Asia/Ho_Chi_Minh' starts_at,m.duration,
 s.price+CASE WHEN extract(isodow FROM date '2026-10-31'+d) IN (6,7) THEN 10000 ELSE 0 END price_minor
 FROM rooms a CROSS JOIN generate_series(0,6) d CROSS JOIN slots s
 JOIN movies m ON m.n=mod(a.n*4+d*7+s.n,32)
 WHERE (SELECT count(*) FROM cinema_cinema.seats st WHERE st.auditorium_id=a.id AND st.is_active AND st.seat_type IN ('STANDARD','VIP'))=100
), new_showtimes AS (
 INSERT INTO cinema_cinema.showtimes(id,auditorium_id,movie_id,starts_at,ends_at,sales_close_at,price_minor,currency,status,snapshot_version,created_at,updated_at)
 SELECT c.id,c.auditorium_id,c.movie_id,c.starts_at,c.starts_at+make_interval(mins=>c.duration),c.starts_at-interval '15 minutes',c.price_minor,'VND','PUBLISHED',1,now(),now()
 FROM candidates c WHERE c.starts_at>now()+interval '15 minutes'
 AND NOT EXISTS (SELECT 1 FROM cinema_cinema.showtimes old WHERE old.auditorium_id=c.auditorium_id AND old.status='PUBLISHED'
 AND tstzrange(old.starts_at,old.ends_at,'[)') && tstzrange(c.starts_at-interval '20 minutes',c.starts_at+make_interval(mins=>c.duration)+interval '20 minutes','[)'))
 ON CONFLICT DO NOTHING RETURNING *
), new_events AS (
 INSERT INTO cinema_cinema.outbox_events(event_id,event_type,event_version,aggregate_id,occurred_at,trace_id,payload,publish_attempts,next_attempt_at,created_at)
 SELECT md5(s.id::text||'-published-v1')::uuid,'ShowtimePublished',1,s.id,now(),'demo-hcm-20261031',
 jsonb_build_object('showtimeId',s.id,'cinemaId',a.cinema_id,'auditoriumId',s.auditorium_id,'movieId',s.movie_id,
 'startsAt',to_char(s.starts_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS"Z"'),
 'salesCloseAt',to_char(s.sales_close_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS"Z"'),
 'currency',s.currency,'snapshotVersion',s.snapshot_version,'seats',
 (SELECT jsonb_agg(jsonb_build_object('seatId',st.id,'label',st.row_label||st.seat_number,'type',st.seat_type,'priceMinor',s.price_minor) ORDER BY st.row_label,st.seat_number)
 FROM cinema_cinema.seats st WHERE st.auditorium_id=s.auditorium_id AND st.is_active)),0,now(),now()
 FROM new_showtimes s JOIN locked_rooms a ON a.id=s.auditorium_id RETURNING event_id
)
SELECT (SELECT count(*) FROM new_showtimes) showtimes_added,(SELECT count(*) FROM new_events) outbox_events_added;

