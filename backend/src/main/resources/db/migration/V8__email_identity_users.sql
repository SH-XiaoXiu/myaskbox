-- ============================================================
-- V8: 用户身份改为邮箱，username 与 email 强制保持一致
-- ============================================================

ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS sys_user_username_key;
DROP INDEX IF EXISTS ux_sys_user_email_lower;

ALTER TABLE sys_user
    ALTER COLUMN username TYPE VARCHAR(200);

WITH candidates AS (
    SELECT
        id,
        CASE
            WHEN email IS NOT NULL
                AND btrim(email) <> ''
                AND btrim(email) ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$'
                THEN lower(btrim(email))
            WHEN username IS NOT NULL
                AND btrim(username) <> ''
                AND btrim(username) ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$'
                THEN lower(btrim(username))
            ELSE concat(
                coalesce(
                    nullif(
                        trim(both '-' FROM regexp_replace(lower(coalesce(btrim(username), '')), '[^a-z0-9]+', '-', 'g')),
                        ''
                    ),
                    'user-' || id
                ),
                '@askbox.local'
            )
        END AS candidate,
        CASE
            WHEN email IS NOT NULL
                AND btrim(email) <> ''
                AND btrim(email) ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$'
                THEN 1
            WHEN username IS NOT NULL
                AND btrim(username) <> ''
                AND btrim(username) ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$'
                THEN 2
            ELSE 3
        END AS priority
    FROM sys_user
),
ranked AS (
    SELECT
        id,
        candidate,
        row_number() OVER (PARTITION BY candidate ORDER BY priority, id) AS candidate_rank
    FROM candidates
),
resolved AS (
    SELECT
        id,
        CASE
            WHEN candidate_rank = 1 THEN candidate
            ELSE concat('legacy-user-', id, '-', substr(md5(id::text || candidate), 1, 8), '@askbox.local')
        END AS final_email
    FROM ranked
)
UPDATE sys_user u
SET
    username = r.final_email,
    email = r.final_email
FROM resolved r
WHERE u.id = r.id;

ALTER TABLE sys_user
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN email SET NOT NULL;

ALTER TABLE sys_user
    ADD CONSTRAINT ck_sys_user_email_identity CHECK (username = email),
    ADD CONSTRAINT ck_sys_user_email_lowercase CHECK (username = lower(username) AND email = lower(email)),
    ADD CONSTRAINT ck_sys_user_email_not_blank CHECK (username <> '' AND email <> '');

CREATE UNIQUE INDEX ux_sys_user_username_lower
    ON sys_user (lower(username));

CREATE UNIQUE INDEX ux_sys_user_email_lower
    ON sys_user (lower(email));
