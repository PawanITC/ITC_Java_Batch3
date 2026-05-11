-- V2: Ensure Admin has correct credentials
-- Wrapped in a guard so this never fails if users table doesn't exist yet
-- (e.g. Flyway history out of sync with actual schema after a partial DB wipe)
DO
$$
BEGIN
    IF
EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'users'
    ) THEN
UPDATE users
SET password = '$2a$10$bMpz3LUN00UeYQZ.01sApejM90Ol87Xn64UmAT2wlyRHNkcsx93b.',
    role     = 'ROLE_ADMIN'
WHERE email = 'admin@funkart.com';
END IF;
END $$;