-- ============================================================
-- V14: Topic limits, settings and hot-path indexes
-- ============================================================

INSERT INTO sys_setting (key, value, value_type, group_code, label, description, is_secret)
VALUES ('topic.active_limit', '5', 'NUMBER', 'topic', '同时有效话题数', '每个提问箱允许同时启用且未到期的话题数量', FALSE)
ON CONFLICT (key) DO NOTHING;

ALTER TABLE box_topics
    ALTER COLUMN title TYPE VARCHAR(24) USING left(title, 24);

CREATE INDEX IF NOT EXISTS idx_box_topics_box_status_recent
    ON box_topics(box_user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_box_topics_box_active_count
    ON box_topics(box_user_id, status, expires_at);

CREATE INDEX IF NOT EXISTS idx_questions_box_topic
    ON questions(box_user_id, topic_id);

CREATE INDEX IF NOT EXISTS idx_questions_box_topic_published
    ON questions(box_user_id, topic_id, created_at DESC)
    WHERE status = 'PUBLISHED';
