-- ============================================================
-- V9: 箱主邮件通知 + 一次性免登录回复 token
-- ============================================================

ALTER TABLE box_user
    ADD COLUMN email_notify_enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE question_reply_tokens (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    box_user_id BIGINT NOT NULL,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_reply_tokens_question ON question_reply_tokens(question_id);
CREATE INDEX idx_question_reply_tokens_box ON question_reply_tokens(box_user_id);
CREATE INDEX idx_question_reply_tokens_expires ON question_reply_tokens(expires_at);

INSERT INTO sys_setting (key, value, value_type, group_code, label, description, is_secret)
VALUES
('site.public_url', '', 'STRING', 'site', '站点外部地址', '邮件通知中使用的前端访问地址，例如 https://askbox.example.com', FALSE)
ON CONFLICT (key) DO NOTHING;
