package cn.xiuxius.askbox.attachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import cn.xiuxius.askbox.attachment.config.ObjectStorageProperties;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.view.UploadPresignView;

import io.minio.MinioClient;
import io.minio.PostPolicy;

@ExtendWith(MockitoExtension.class)
class MinioObjectStorageServiceTest {

    @Mock
    private MinioClient client;

    @Test
    void redirectAssetResponseUsesLongTermCacheHeaders() {
        MinioObjectStorageService redirectService =
                new MinioObjectStorageService(redirectProperties(ObjectStorageProperties.AccessMode.REDIRECT));

        ResponseEntity<?> response =
                redirectService.assetResponse("uploads/account_avatar/1/avatar.png", "image/png", 128L, "abc123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://cdn.example.com/askbox-assets/uploads/account_avatar/1/avatar.png");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=31536000, public, immutable");
    }

    @Test
    void presignUploadIncludesCacheControlMetadata() throws Exception {
        MinioObjectStorageService redirectService =
                new MinioObjectStorageService(redirectProperties(ObjectStorageProperties.AccessMode.REDIRECT));
        Map<String, String> signed = Map.of("policy", "signed-policy", "x-amz-signature", "sig");
        doReturn(signed).when(client).getPresignedPostFormData(any(PostPolicy.class));
        setClient(redirectService, client);

        UploadPresignView view =
                redirectService.presignUpload(AttachmentUsageType.ACCOUNT_AVATAR, "avatar.png", "image/png", 1024, 42L);

        assertThat(view.objectKey()).startsWith("uploads/account_avatar/42/");
        assertThat(view.uploadUrl()).isEqualTo("http://cdn.example.com/askbox-assets");
        assertThat(view.formData())
                .containsEntry("Content-Type", "image/png")
                .containsEntry("Cache-Control", "public, max-age=31536000, immutable");
        assertThat(view.expiresAt()).isAfter(OffsetDateTime.now().minusSeconds(5));
    }

    @Test
    void proxyAssetResponseIncludesEntityCachingHeaders() throws Exception {
        MinioObjectStorageService proxyService =
                spy(new MinioObjectStorageService(redirectProperties(ObjectStorageProperties.AccessMode.PROXY)));
        setClient(proxyService, client);
        doReturn(new MinioObjectStorageService.InputStreamHolder(
                        new ByteArrayInputStream(new byte[] {1, 2, 3}), "image/webp"))
                .when(proxyService)
                .getObject(any());

        ResponseEntity<?> response =
                proxyService.assetResponse("uploads/account_avatar/1/avatar.webp", "image/webp", 3L, "etag-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("image/webp");
        assertThat(response.getHeaders().getFirst(HttpHeaders.ETAG)).isEqualTo("\"etag-123\"");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=31536000, public, immutable");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(3L);
        assertThat(response.getBody()).isEqualTo(new byte[] {1, 2, 3});
    }

    private static ObjectStorageProperties redirectProperties(ObjectStorageProperties.AccessMode accessMode) {
        return new ObjectStorageProperties(
                "http://localhost:9000",
                "http://cdn.example.com",
                "key",
                "secret",
                "askbox-assets",
                "us-east-1",
                accessMode,
                600,
                31_536_000);
    }

    private static void setClient(MinioObjectStorageService service, MinioClient client) throws Exception {
        var field = MinioObjectStorageService.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(service, client);
    }
}
