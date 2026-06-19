package cn.xiuxius.askbox.boxuser.view;

import java.time.OffsetDateTime;

import cn.xiuxius.askbox.attachment.view.AttachmentView;

public record BoxProfileView(
        Long id,
        Long userId,
        String slug,
        String displayName,
        String ownerDisplayName,
        String description,
        AttachmentView avatar,
        AttachmentView background,
        Boolean emailNotifyEnabled,
        OffsetDateTime createdAt) {}
