-- V1: Initial Schema and Root Admin Seeding
CREATE TABLE IF NOT EXISTS users
(
    id         SERIAL PRIMARY KEY,
    email      VARCHAR(255) UNIQUE NOT NULL,
    name       VARCHAR(255),
    password   VARCHAR(255)        NOT NULL,
    role       VARCHAR(50)         NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed Root Admin (Password: admin123)
INSERT INTO users (email, name, password, role)
SELECT 'admin@funkart.com',
       'System Root',
       '$2a$10$bMpz3LUN00UeYQZ.01sApejM90Ol87Xn64UmAT2wlyRHNkcsx93b.',
       'ROLE_ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@funkart.com'
);