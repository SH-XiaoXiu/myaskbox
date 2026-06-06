package cn.xiuxius.askbox.system.view;

import java.time.OffsetDateTime;

/**
 * 角色视图——额外包含拥有该角色的用户数量。
 */
public record RoleView(
        Long id,
        String code,
        String name,
        String description,
        Boolean builtIn,
        long userCount,
        OffsetDateTime createdAt) {}
