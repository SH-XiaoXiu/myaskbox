-- ============================================================
-- V5: 为无角色普通用户补充 BOX_OWNER 角色
-- ============================================================

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'BOX_OWNER'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_role ur
    WHERE ur.user_id = u.id
);
