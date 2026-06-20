package cn.xiuxius.askbox.attachment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "askbox.object-storage")
public record ObjectStorageProperties(
        String endpoint,
        String publicEndpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String region,
        AccessMode accessMode,
        int presignTtlSeconds,
        int assetCacheMaxAgeSeconds) {

    public ObjectStorageProperties {
        if (bucket == null || bucket.isBlank()) {
            bucket = "askbox-assets";
        }
        if (accessMode == null) {
            accessMode = AccessMode.REDIRECT;
        }
        if (presignTtlSeconds <= 0) {
            presignTtlSeconds = 600;
        }
        if (assetCacheMaxAgeSeconds <= 0) {
            assetCacheMaxAgeSeconds = 31_536_000;
        }
    }

    public enum AccessMode {
        REDIRECT,
        PROXY
    }
}
