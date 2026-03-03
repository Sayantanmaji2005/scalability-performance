ALTER TABLE users
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS email_verification_token_hash VARCHAR(128);

ALTER TABLE users
ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMPTZ;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS password_reset_token_hash VARCHAR(128);

ALTER TABLE users
ADD COLUMN IF NOT EXISTS password_reset_expires_at TIMESTAMPTZ;

UPDATE users
SET email_verified = TRUE
WHERE email_verified = FALSE;
