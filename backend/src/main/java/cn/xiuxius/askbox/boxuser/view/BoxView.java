package cn.xiuxius.askbox.boxuser.view;

import java.time.OffsetDateTime;

/**
 * 管理端提问箱列表视图——额外包含 owner 账号邮箱和问题总数。
 */
public record BoxView(
        Long id,
        String username,
        String slug,
        String displayName,
        String description,
        long questionCount,
        OffsetDateTime createdAt) {}
