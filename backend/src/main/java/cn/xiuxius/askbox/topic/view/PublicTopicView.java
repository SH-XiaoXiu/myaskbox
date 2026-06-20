package cn.xiuxius.askbox.topic.view;

import java.time.OffsetDateTime;

public record PublicTopicView(
        String code,
        String title,
        String description,
        Boolean available,
        String unavailableReason,
        OffsetDateTime expiresAt) {}
