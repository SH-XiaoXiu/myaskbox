package cn.xiuxius.askbox.attachment.service;

import org.springframework.http.ResponseEntity;

import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.view.UploadPresignView;

public interface ObjectStorageService {

    UploadPresignView presignUpload(
            AttachmentUsageType usageType, String fileName, String mimeType, long sizeBytes, Long userId);

    StoredObject inspectUploadedObject(String objectKey, AttachmentUsageType usageType);

    void putObject(String objectKey, byte[] bytes, String mimeType);

    ResponseEntity<?> assetResponse(String objectKey);

    record StoredObject(String objectKey, String mimeType, Long sizeBytes, String sha256) {}
}
