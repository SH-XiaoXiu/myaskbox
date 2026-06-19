package cn.xiuxius.askbox.attachment.view;

import cn.xiuxius.askbox.attachment.enums.AttachmentStorageType;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;

public record AttachmentView(
        Long id,
        String name,
        AttachmentUsageType usageType,
        AttachmentStorageType storageType,
        String objectKey,
        String mimeType,
        Long sizeBytes,
        String bg,
        Integer sortOrder,
        Boolean isActive) {}
