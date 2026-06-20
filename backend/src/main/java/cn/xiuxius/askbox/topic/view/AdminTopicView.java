package cn.xiuxius.askbox.topic.view;

import java.time.OffsetDateTime;

public record AdminTopicView(
        Long id,
        String code,
        String title,
        String description,
        String status,
        Boolean available,
        OffsetDateTime expiresAt,
        OffsetDateTime closedAt,
        OffsetDateTime createdAt,
        Long questionCount,
        Long boxUserId,
        String boxSlug,
        String boxDisplayName,
        Long ownerUserId,
        String ownerEmail,
        String ownerDisplayName) {}
