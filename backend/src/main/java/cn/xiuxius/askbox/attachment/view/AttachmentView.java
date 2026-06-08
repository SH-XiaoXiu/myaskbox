package cn.xiuxius.askbox.attachment.view;

import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;

public record AttachmentView(
        Long id,
        String name,
        AttachmentUsageType usageType,
        String contentBase64,
        String mimeType,
        Long sizeBytes,
        String bg,
        Integer sortOrder,
        Boolean isActive) {}
