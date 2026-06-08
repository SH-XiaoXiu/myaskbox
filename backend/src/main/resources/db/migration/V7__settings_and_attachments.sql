-- ============================================================
-- V7: 系统设置 + 附件库，强迁移替换头像库
-- ============================================================

CREATE TABLE sys_setting (
    key         VARCHAR(100) PRIMARY KEY,
    value       TEXT,
    value_type  VARCHAR(20) NOT NULL DEFAULT 'STRING',
    group_code  VARCHAR(50) NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_secret   BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO sys_setting (key, value, value_type, group_code, label, description, is_secret) VALUES
('registration.enabled', 'false', 'BOOLEAN', 'registration', '开放注册', '是否允许访客通过邮箱验证码注册箱主账号', FALSE),
('mail.host', '', 'STRING', 'mail', 'SMTP Host', '邮件服务器地址', FALSE),
('mail.port', '465', 'NUMBER', 'mail', 'SMTP Port', '邮件服务器端口', FALSE),
('mail.username', '', 'STRING', 'mail', 'SMTP 用户名', 'SMTP 登录用户名', FALSE),
('mail.password', '', 'STRING', 'mail', 'SMTP 密码', 'SMTP 登录密码，明文存储，接口展示时脱敏', TRUE),
('mail.from', '', 'STRING', 'mail', '发件人邮箱', '注册验证码邮件 From 地址，通常需要是 SMTP 账号或已授权别名', FALSE),
('mail.from_name', 'AskBox', 'STRING', 'mail', '发件显示名称', '收件箱中显示在发件地址前面的名称，可留空', FALSE),
('mail.ssl_enabled', 'true', 'BOOLEAN', 'mail', '启用 SSL', 'SMTP 是否启用 SSL', FALSE);

CREATE TABLE attachments (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    usage_type     VARCHAR(50) NOT NULL,
    storage_type   VARCHAR(20) NOT NULL DEFAULT 'BASE64',
    content_base64 TEXT NOT NULL,
    mime_type      VARCHAR(100) NOT NULL,
    size_bytes     BIGINT NOT NULL DEFAULT 0,
    sha256         VARCHAR(64) NOT NULL,
    bg             VARCHAR(50),
    sort_order     INT NOT NULL DEFAULT 0,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    owner_type     VARCHAR(50),
    owner_id       BIGINT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_usage_active_sort ON attachments(usage_type, is_active, sort_order);
CREATE INDEX idx_attachments_owner ON attachments(owner_type, owner_id);
CREATE INDEX idx_attachments_sha256 ON attachments(sha256);

INSERT INTO attachments (
    id, name, usage_type, storage_type, content_base64, mime_type, size_bytes, sha256,
    bg, sort_order, is_active, created_at, updated_at
)
SELECT
    id,
    name,
    'ANONYMOUS_AVATAR',
    'BASE64',
    icon_base64,
    CASE
        WHEN icon_base64 LIKE 'data:image/svg+xml%' THEN 'image/svg+xml'
        WHEN icon_base64 LIKE 'data:image/png%' THEN 'image/png'
        WHEN icon_base64 LIKE 'data:image/jpeg%' THEN 'image/jpeg'
        WHEN icon_base64 LIKE 'data:image/webp%' THEN 'image/webp'
        WHEN icon_base64 LIKE 'data:image/gif%' THEN 'image/gif'
        ELSE 'application/octet-stream'
    END,
    length(icon_base64),
    md5(icon_base64),
    bg,
    sort_order,
    is_active,
    created_at,
    updated_at
FROM avatars;

SELECT setval('attachments_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM attachments), 1));

ALTER TABLE questions RENAME COLUMN avatar_id TO attachment_id;

ALTER TABLE box_user
    ADD COLUMN avatar_attachment_id BIGINT,
    ADD COLUMN background_attachment_id BIGINT;

CREATE UNIQUE INDEX ux_sys_user_email_lower
    ON sys_user (lower(email))
    WHERE email IS NOT NULL AND email <> '';

UPDATE sys_permission SET code = 'admin:attachment', name = '附件管理' WHERE code = 'admin:avatar';
UPDATE sys_permission SET code = 'admin:attachment:list', name = '查看附件列表' WHERE code = 'admin:avatar:list';
UPDATE sys_permission SET code = 'admin:attachment:create', name = '新增附件' WHERE code = 'admin:avatar:create';
UPDATE sys_permission SET code = 'admin:attachment:edit', name = '编辑附件' WHERE code = 'admin:avatar:edit';
UPDATE sys_permission SET code = 'admin:attachment:delete', name = '删除附件' WHERE code = 'admin:avatar:delete';

DROP TABLE avatars;
