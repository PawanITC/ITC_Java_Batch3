-- V2: Ensure Admin has correct credentials
UPDATE users
SET password = '$2a$10$bMpz3LUN00UeYQZ.01sApejM90Ol87Xn64UmAT2wlyRHNkcsx93b.',
    role = 'ROLE_ADMIN'
WHERE email = 'admin@funkart.com';