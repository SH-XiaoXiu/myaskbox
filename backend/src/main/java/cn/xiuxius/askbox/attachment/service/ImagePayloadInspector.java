package cn.xiuxius.askbox.attachment.service;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import cn.hutool.crypto.digest.DigestUtil;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;

public final class ImagePayloadInspector {

    private static final long AVATAR_LIMIT = 256L * 1024;
    private static final long BACKGROUND_LIMIT = 1024L * 1024;

    private ImagePayloadInspector() {}

    public static ImagePayload inspect(byte[] bytes, String declaredMime, AttachmentUsageType usageType) {
        if (bytes == null || bytes.length == 0) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "附件内容不能为空");
        }
        long maxBytes = usageType == AttachmentUsageType.BOX_BACKGROUND ? BACKGROUND_LIMIT : AVATAR_LIMIT;
        if (bytes.length > maxBytes) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "附件大小超过限制");
        }
        String actualMime = detectMime(bytes, usageType);
        if (declaredMime != null && !declaredMime.isBlank() && !actualMime.equalsIgnoreCase(declaredMime)) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "附件声明格式与真实格式不一致");
        }
        return new ImagePayload(actualMime, (long) bytes.length, DigestUtil.sha256Hex(bytes), extension(actualMime));
    }

    public static long maxBytes(AttachmentUsageType usageType) {
        return usageType == AttachmentUsageType.BOX_BACKGROUND ? BACKGROUND_LIMIT : AVATAR_LIMIT;
    }

    public static String extension(String mimeType) {
        return switch (mimeType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "image/svg+xml" -> "svg";
            default -> "bin";
        };
    }

    private static String detectMime(byte[] bytes, AttachmentUsageType usageType) {
        if (bytes.length >= 8 && HexFormat.of().formatHex(bytes, 0, 8).equals("89504e470d0a1a0a")) {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (bytes.length >= 12 && new String(bytes, 0, 4).equals("RIFF") && new String(bytes, 8, 4).equals("WEBP")) {
            return "image/webp";
        }
        if (bytes.length >= 6) {
            String signature = new String(bytes, 0, 6);
            if ("GIF87a".equals(signature) || "GIF89a".equals(signature)) {
                return "image/gif";
            }
        }
        if (usageType == AttachmentUsageType.ANONYMOUS_AVATAR && looksLikeSvg(bytes)) {
            return "image/svg+xml";
        }
        String supported =
                usageType == AttachmentUsageType.ANONYMOUS_AVATAR ? "PNG、JPEG、WebP、GIF、SVG" : "PNG、JPEG、WebP、GIF";
        throw new BizException(ErrorCodes.VALIDATION_ERROR, "仅支持 " + supported + " 图片");
    }

    private static boolean looksLikeSvg(byte[] bytes) {
        String value = new String(bytes, StandardCharsets.UTF_8).trim().toLowerCase();
        return value.startsWith("<svg") || value.startsWith("<?xml") && value.contains("<svg");
    }

    public record ImagePayload(String mimeType, Long sizeBytes, String sha256, String extension) {}
}
