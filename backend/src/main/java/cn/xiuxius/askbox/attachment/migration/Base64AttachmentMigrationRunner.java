package cn.xiuxius.askbox.attachment.migration;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cn.xiuxius.askbox.attachment.enums.AttachmentStorageType;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.service.Base64ImageInspector;
import cn.xiuxius.askbox.attachment.service.Base64ImageInspector.DecodedImage;
import cn.xiuxius.askbox.attachment.service.ObjectStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("migrate-attachments")
@RequiredArgsConstructor
@Slf4j
public class Base64AttachmentMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectStorageService objectStorageService;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean dryRun = args.containsOption("dry-run");
        List<LegacyAttachment> rows = jdbcTemplate.query(
                """
                SELECT id, usage_type, content_base64
                FROM attachments
                WHERE content_base64 IS NOT NULL
                  AND content_base64 <> ''
                  AND (object_key IS NULL OR object_key = '')
                ORDER BY id
                """,
                (ResultSet rs, int rowNum) -> new LegacyAttachment(
                        rs.getLong("id"),
                        AttachmentUsageType.valueOf(rs.getString("usage_type")),
                        rs.getString("content_base64")));
        log.info("Found {} legacy base64 attachments to migrate. dryRun={}", rows.size(), dryRun);
        long totalBytes = 0;
        for (LegacyAttachment row : rows) {
            DecodedImage image;
            try {
                image = Base64ImageInspector.inspectDataUrl(row.contentBase64(), row.usageType());
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Invalid legacy attachment base64: id=" + row.id(), ex);
            }
            totalBytes += image.payload().sizeBytes();
            String objectKey = "migrated/attachments/%d-%s.%s"
                    .formatted(
                            row.id(), image.payload().sha256(), image.payload().extension());
            log.info(
                    "Attachment {} => key={} mime={} size={}",
                    row.id(),
                    objectKey,
                    image.payload().mimeType(),
                    image.payload().sizeBytes());
            if (dryRun) {
                continue;
            }
            objectStorageService.putObject(
                    objectKey, image.bytes(), image.payload().mimeType());
            int updated = jdbcTemplate.update(
                    """
                    UPDATE attachments
                    SET storage_type = ?,
                        object_key = ?,
                        mime_type = ?,
                        size_bytes = ?,
                        sha256 = ?,
                        updated_at = ?
                    WHERE id = ?
                    """,
                    AttachmentStorageType.S3.name(),
                    objectKey,
                    image.payload().mimeType(),
                    image.payload().sizeBytes(),
                    image.payload().sha256(),
                    OffsetDateTime.now(),
                    row.id());
            if (updated != 1) {
                throw new IllegalStateException("Attachment migration update failed: id=" + row.id());
            }
        }
        log.info(
                "Legacy attachment migration finished. count={} totalBytes={} dryRun={}",
                rows.size(),
                totalBytes,
                dryRun);
        Thread.ofVirtual().start(applicationContext::close);
    }

    private record LegacyAttachment(Long id, AttachmentUsageType usageType, String contentBase64) {}
}
