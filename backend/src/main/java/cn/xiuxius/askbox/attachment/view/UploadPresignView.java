package cn.xiuxius.askbox.attachment.view;

import java.time.OffsetDateTime;
import java.util.Map;

public record UploadPresignView(
        String objectKey, String uploadUrl, Map<String, String> formData, OffsetDateTime expiresAt) {}
