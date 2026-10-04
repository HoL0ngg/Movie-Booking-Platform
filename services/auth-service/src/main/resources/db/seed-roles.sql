INSERT INTO roles (id, code, created_at)
VALUES -- 0.11 Seed role mặc định
    (gen_random_uuid(), 'CUSTOMER', now()),
    -- 0.12 gen_random_uuid(): hàm UUID có sẵn của PostgreSQL
    (gen_random_uuid(), 'ADMIN', now()),
    -- 0.13
    (gen_random_uuid(), 'CINEMA_MANAGER', now()) -- 0.14
    ON CONFLICT (code) DO NOTHING;
-- 0.15 Chạy lại không lỗi vì code UNIQUE