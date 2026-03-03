CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (username, password, email, role, enabled, created_at, updated_at)
VALUES (
    'demo@scalemart.dev',
    crypt('DemoPass123!', gen_salt('bf')),
    'demo@scalemart.dev',
    'USER',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username)
DO UPDATE SET
    password = EXCLUDED.password,
    email = EXCLUDED.email,
    role = EXCLUDED.role,
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO users (username, password, email, role, enabled, created_at, updated_at)
VALUES (
    'admin@scalemart.dev',
    crypt('AdminPass123!', gen_salt('bf')),
    'admin@scalemart.dev',
    'ADMIN',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username)
DO UPDATE SET
    password = EXCLUDED.password,
    email = EXCLUDED.email,
    role = EXCLUDED.role,
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP;
