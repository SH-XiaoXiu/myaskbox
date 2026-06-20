ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS topic_active_limit INT NOT NULL DEFAULT 5;

UPDATE sys_user
SET topic_active_limit = 5
WHERE topic_active_limit IS NULL OR topic_active_limit < 1;

UPDATE box_topics
SET expires_at = created_at + INTERVAL '24 hours'
WHERE expires_at IS NULL;

ALTER TABLE box_topics
    ALTER COLUMN expires_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sys_user_topic_active_limit ON sys_user(topic_active_limit);

DELETE FROM sys_setting
WHERE key = 'topic.active_limit';
