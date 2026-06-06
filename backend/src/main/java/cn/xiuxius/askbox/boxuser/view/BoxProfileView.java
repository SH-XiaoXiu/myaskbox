package cn.xiuxius.askbox.boxuser.view;

import java.time.OffsetDateTime;

public record BoxProfileView(
        Long id, Long userId, String slug, String displayName, String description, OffsetDateTime createdAt) {}
