package cn.xiuxius.askbox.system.view;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 管理端用户视图——额外包含角色列表。
 */
public record UserView(
        Long id,
        String username,
        String displayName,
        String email,
        String status,
        List<String> roles,
        OffsetDateTime createdAt) {}
