CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS devices (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_type TEXT NOT NULL CHECK (device_type IN ('android', 'desktop', 'watch')),
    name TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS screentime_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES devices(user_id) ON DELETE CASCADE,
    activity TEXT NOT NULL,
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    device_type TEXT NOT NULL CHECK (device_type IN ('android', 'desktop','watch')),
    created_at TIMESTAMPTZ DEFAULT now()
);

SELECT * FROM screentime_logs ORDER by start_time desc;
