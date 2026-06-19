package cn.xiuxius.askbox.attachment.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import cn.xiuxius.askbox.attachment.config.ObjectStorageProperties;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.service.ImagePayloadInspector.ImagePayload;
import cn.xiuxius.askbox.attachment.view.UploadPresignView;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioObjectStorageService implements ObjectStorageService {

    private final ObjectStorageProperties properties;

    private MinioClient client;

    @PostConstruct
    void init() {
        client = MinioClient.builder()
                .endpoint(required(properties.endpoint(), "MinIO endpoint"))
                .credentials(
                        required(properties.accessKey(), "MinIO access key"),
                        required(properties.secretKey(), "MinIO secret key"))
                .region(properties.region())
                .build();
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.bucket()).build());
            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(properties.bucket()).build());
            }
            if (properties.accessMode() == ObjectStorageProperties.AccessMode.REDIRECT) {
                client.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(properties.bucket())
                        .config(publicReadPolicy())
                        .build());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("MinIO 初始化失败", ex);
        }
    }

    @Override
    public UploadPresignView presignUpload(
            AttachmentUsageType usageType, String fileName, String mimeType, long sizeBytes, Long userId) {
        validateDeclaredUpload(usageType, mimeType, sizeBytes);
        String objectKey = objectKey(usageType, fileName, userId);
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(properties.presignTtlSeconds());
        try {
            PostPolicy policy = new PostPolicy(
                    properties.bucket(), ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(properties.presignTtlSeconds()));
            policy.addEqualsCondition("key", objectKey);
            policy.addEqualsCondition("Content-Type", mimeType);
            policy.addContentLengthRangeCondition(1, ImagePayloadInspector.maxBytes(usageType));
            Map<String, String> formData = new HashMap<>(client.getPresignedPostFormData(policy));
            formData.put("key", objectKey);
            formData.put("Content-Type", mimeType);
            return new UploadPresignView(objectKey, bucketUrl(), formData, expiresAt);
        } catch (Exception ex) {
            throw new BizException(ErrorCodes.INTERNAL_ERROR, "生成上传签名失败");
        }
    }

    @Override
    public StoredObject inspectUploadedObject(String objectKey, AttachmentUsageType usageType) {
        assertObjectKey(objectKey);
        try (InputStreamHolder holder = getObject(objectKey)) {
            byte[] bytes = StreamUtils.copyToByteArray(holder.stream());
            ImagePayload payload = ImagePayloadInspector.inspect(bytes, holder.contentType(), usageType);
            return new StoredObject(objectKey, payload.mimeType(), payload.sizeBytes(), payload.sha256());
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Uploaded object validation failed: key={}", objectKey, ex);
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "上传对象不存在或不可读取");
        }
    }

    @Override
    public void putObject(String objectKey, byte[] bytes, String mimeType) {
        assertObjectKey(objectKey);
        try {
            client.putObject(
                    PutObjectArgs.builder().bucket(properties.bucket()).object(objectKey).contentType(mimeType).stream(
                                    new ByteArrayInputStream(bytes), (long) bytes.length, -1L)
                            .build());
        } catch (Exception ex) {
            throw new BizException(ErrorCodes.INTERNAL_ERROR, "上传对象失败");
        }
    }

    @Override
    public ResponseEntity<?> assetResponse(String objectKey) {
        assertObjectKey(objectKey);
        if (properties.accessMode() == ObjectStorageProperties.AccessMode.REDIRECT) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(objectUrl(objectKey)))
                    .cacheControl(CacheControl.noCache())
                    .build();
        }
        try (InputStreamHolder holder = getObject(objectKey)) {
            byte[] bytes = StreamUtils.copyToByteArray(holder.stream());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, holder.contentType())
                    .cacheControl(CacheControl.noCache())
                    .body(bytes);
        } catch (Exception ex) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "图片不存在");
        }
    }

    private InputStreamHolder getObject(String objectKey) throws Exception {
        var object = client.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectKey)
                .build());
        String contentType = object.headers().get("Content-Type");
        return new InputStreamHolder(object, contentType);
    }

    private void validateDeclaredUpload(AttachmentUsageType usageType, String mimeType, long sizeBytes) {
        if (sizeBytes <= 0 || sizeBytes > ImagePayloadInspector.maxBytes(usageType)) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "附件大小超过限制");
        }
        if (!allowedMime(usageType, mimeType)) {
            String supported =
                    usageType == AttachmentUsageType.ANONYMOUS_AVATAR ? "PNG、JPEG、WebP、GIF、SVG" : "PNG、JPEG、WebP、GIF";
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "仅支持 " + supported + " 图片");
        }
    }

    private boolean allowedMime(AttachmentUsageType usageType, String mimeType) {
        if (mimeType == null) return false;
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "image/png", "image/jpeg", "image/webp", "image/gif" -> true;
            case "image/svg+xml" -> usageType == AttachmentUsageType.ANONYMOUS_AVATAR;
            default -> false;
        };
    }

    private String objectKey(AttachmentUsageType usageType, String fileName, Long userId) {
        String ext = extensionFromFileName(fileName);
        String owner = userId == null ? "anonymous" : String.valueOf(userId);
        return "uploads/%s/%s/%s.%s"
                .formatted(usageType.name().toLowerCase(Locale.ROOT), owner, UUID.randomUUID(), ext);
    }

    private String extensionFromFileName(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "bin";
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return ext.isBlank() ? "bin" : ext;
    }

    private String bucketUrl() {
        return trimSlash(publicEndpoint()) + "/" + encodeSegment(properties.bucket());
    }

    private String objectUrl(String objectKey) {
        StringBuilder value = new StringBuilder(bucketUrl());
        for (String segment : objectKey.split("/")) {
            value.append('/').append(encodeSegment(segment));
        }
        return value.toString();
    }

    private String publicEndpoint() {
        return properties.publicEndpoint() == null
                        || properties.publicEndpoint().isBlank()
                ? properties.endpoint()
                : properties.publicEndpoint();
    }

    private String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String trimSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private void assertObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || objectKey.startsWith("/") || objectKey.contains("..")) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "对象 key 不合法");
        }
    }

    private String publicReadPolicy() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::%s/*"]
                  }]
                }
                """
                .formatted(properties.bucket());
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " 未配置");
        }
        return value;
    }

    private record InputStreamHolder(io.minio.GetObjectResponse stream, String contentType) implements AutoCloseable {
        InputStreamHolder {
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
        }

        @Override
        public void close() throws Exception {
            stream.close();
        }
    }
}
