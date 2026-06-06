-- ============================================================
-- V1: 系统用户、角色、权限、用户角色关联、角色权限关联
-- ============================================================

-- 系统用户表
CREATE TABLE sys_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100),
    email           VARCHAR(200),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMPTZ,
    last_login_ip   VARCHAR(50),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 角色表
CREATE TABLE sys_role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    built_in    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 权限表（支持树形分组）
CREATE TABLE sys_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    parent_id   BIGINT,
    type        VARCHAR(20) NOT NULL DEFAULT 'ACTION',
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 用户-角色关联
CREATE TABLE sys_user_role (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    UNIQUE(user_id, role_id)
);

-- 角色-权限关联
CREATE TABLE sys_role_permission (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT NOT NULL REFERENCES sys_role(id),
    permission_id BIGINT NOT NULL REFERENCES sys_permission(id),
    UNIQUE(role_id, permission_id)
);

-- ============================================================
-- 种子数据: 角色
-- ============================================================
INSERT INTO sys_role (id, code, name, description, built_in) VALUES
(1, 'ADMIN', '系统管理员', '拥有所有权限，管理用户、提问箱和全部数据', TRUE),
(2, 'BOX_OWNER', '提问箱主', '管理自己的提问箱，回答和驳回问题', TRUE);
SELECT setval('sys_role_id_seq', 2);

-- ============================================================
-- 种子数据: 权限
-- ============================================================
-- 分组 (parent_id=NULL)
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(1, 'box', '提问箱管理', NULL, 'GROUP', 1),
(2, 'question', '问题管理', NULL, 'GROUP', 2),
(3, 'admin', '系统管理', NULL, 'GROUP', 3);

-- box 子权限
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(11, 'box:view', '查看提问箱', 1, 'ACTION', 1),
(12, 'box:edit', '编辑提问箱', 1, 'ACTION', 2);

-- question 子权限
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(21, 'question:view', '查看问题', 2, 'ACTION', 1),
(22, 'question:answer', '回答问题', 2, 'ACTION', 2),
(23, 'question:dismiss', '驳回问题', 2, 'ACTION', 3),
(24, 'question:delete', '删除已驳回问题', 2, 'ACTION', 4);

-- admin 分组子项
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(30, 'admin:user', '用户管理', 3, 'GROUP', 1),
(31, 'admin:box', '提问箱管理', 3, 'GROUP', 2),
(32, 'admin:question', '问题管理', 3, 'GROUP', 3),
(33, 'admin:answer', '回答管理', 3, 'GROUP', 4),
(34, 'admin:role', '角色管理', 3, 'GROUP', 5),
(35, 'admin:permission', '权限管理', 3, 'GROUP', 6),
(36, 'admin:avatar', '头像管理', 3, 'GROUP', 7);

-- admin:user 子权限
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(301, 'admin:user:list', '查看用户列表', 30, 'ACTION', 1),
(302, 'admin:user:create', '创建用户', 30, 'ACTION', 2),
(303, 'admin:user:edit', '编辑用户', 30, 'ACTION', 3),
(304, 'admin:user:delete', '删除用户', 30, 'ACTION', 4),
(305, 'admin:user:assign-role', '分配角色', 30, 'ACTION', 5);

-- admin:box 子权限
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(311, 'admin:box:list', '查看所有提问箱', 31, 'ACTION', 1),
(312, 'admin:box:edit', '编辑任意提问箱', 31, 'ACTION', 2);

-- admin:question 子权限
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(321, 'admin:question:list', '查看所有问题', 32, 'ACTION', 1),
(322, 'admin:question:delete', '强制删除任意问题', 32, 'ACTION', 2);

-- admin:answer 子权限
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(331, 'admin:answer:delete', '删除任意回答', 33, 'ACTION', 1);

-- admin:role 子权限
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(341, 'admin:role:list', '查看角色列表', 34, 'ACTION', 1);

-- admin:permission 子权限
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(351, 'admin:permission:list', '查看权限列表', 35, 'ACTION', 1);

-- admin:avatar 子权限
INSERT INTO sys_permission (id, code, name, parent_id, type, sort_order) VALUES
(361, 'admin:avatar:list', '查看头像列表', 36, 'ACTION', 1),
(362, 'admin:avatar:create', '新增头像', 36, 'ACTION', 2),
(363, 'admin:avatar:edit', '编辑头像', 36, 'ACTION', 3),
(364, 'admin:avatar:delete', '删除头像', 36, 'ACTION', 4);
SELECT setval('sys_permission_id_seq', 400);

-- ============================================================
-- 种子数据: 为 ADMIN 角色分配所有权限
-- ============================================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE type = 'ACTION';

-- ============================================================
-- 种子数据: 为 BOX_OWNER 角色分配 box:* + question:* 权限
-- ============================================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE type = 'ACTION' AND code NOT LIKE 'admin:%';

-- ============================================================
-- 种子数据: 初始管理员用户 (密码: admin123)
-- ============================================================
INSERT INTO sys_user (id, username, password_hash, display_name, status) VALUES
(1, 'admin', '$2b$12$TRT07ceaDKa7ExIYwALCf.Ae1tc7BjcBUOr2cVXZRU03PSyIP.Z0y', '系统管理员', 'ACTIVE');
SELECT setval('sys_user_id_seq', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
