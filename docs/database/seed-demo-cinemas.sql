-- Demo-only cinema catalog. Only newly inserted cinemas receive new rooms/seats.
-- Deterministic IDs make retries a no-op. Existing layouts are never modified.
WITH source(n,name,address) AS (VALUES (1, 'Cinemate Demo Bến Thành', 'Cơ sở mô phỏng tại Bến Thành, TP.HCM'),
(2, 'Cinemate Demo Bình Thạnh', 'Cơ sở mô phỏng tại Bình Thạnh, TP.HCM'),
(3, 'Cinemate Demo Thủ Đức', 'Cơ sở mô phỏng tại Thủ Đức, TP.HCM'),
(4, 'Cinemate Demo Tân Bình', 'Cơ sở mô phỏng tại Tân Bình, TP.HCM'),
(5, 'Cinemate Demo Phú Nhuận', 'Cơ sở mô phỏng tại Phú Nhuận, TP.HCM'),
(6, 'Cinemate Demo Gò Vấp', 'Cơ sở mô phỏng tại Gò Vấp, TP.HCM'),
(7, 'Cinemate Demo Bình Tân', 'Cơ sở mô phỏng tại Bình Tân, TP.HCM'),
(8, 'Cinemate Demo Tân Phú', 'Cơ sở mô phỏng tại Tân Phú, TP.HCM'),
(9, 'Cinemate Demo Phú Mỹ Hưng', 'Cơ sở mô phỏng tại Phú Mỹ Hưng, TP.HCM'),
(10, 'Cinemate Demo Chợ Lớn', 'Cơ sở mô phỏng tại Chợ Lớn, TP.HCM')),
new_cinemas AS (
 INSERT INTO cinema_cinema.cinemas(id,name,address,city,timezone,created_at,updated_at)
 SELECT md5('cinemate-demo-hcm-v1-'||n)::uuid,name,address,'TP. Hồ Chí Minh','Asia/Ho_Chi_Minh',now(),now()
 FROM source s WHERE NOT EXISTS(SELECT 1 FROM cinema_cinema.cinemas c WHERE lower(btrim(c.name))=lower(s.name))
 ON CONFLICT DO NOTHING RETURNING id
), new_rooms AS (
 INSERT INTO cinema_cinema.auditoriums(id,cinema_id,name,created_at,updated_at)
 SELECT md5(c.id::text||'-room-'||r)::uuid,c.id,'Phòng '||r,now(),now()
 FROM new_cinemas c CROSS JOIN generate_series(1,3) r
 RETURNING id
), new_seats AS (
 INSERT INTO cinema_cinema.seats(id,auditorium_id,row_label,seat_number,seat_type,is_active,created_at,updated_at)
 SELECT md5(a.id::text||'-seat-'||r||'-'||n)::uuid,a.id,chr(64+r),n,
 CASE WHEN r BETWEEN 5 AND 7 THEN 'VIP' ELSE 'STANDARD' END,true,now(),now()
 FROM new_rooms a CROSS JOIN generate_series(1,10) r CROSS JOIN generate_series(1,10) n
 RETURNING id
)
SELECT (SELECT count(*) FROM new_cinemas) cinemas_added,
 (SELECT count(*) FROM new_rooms) rooms_added,
 (SELECT count(*) FROM new_seats) seats_added;

