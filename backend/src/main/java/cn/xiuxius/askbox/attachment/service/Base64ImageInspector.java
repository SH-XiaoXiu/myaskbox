package cn.xiuxius.askbox.attachment.service;

import java.util.Base64;
import java.util.Locale;

import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;

public final class Base64ImageInspector {

    private Base64ImageInspector() {}

    public static ImagePayload inspect(String dataUrl, AttachmentUsageType usageType) {
        ImagePayloadInspector.ImagePayload payload =
                inspectDataUrl(dataUrl, usageType).payload();
        return new ImagePayload(payload.mimeType(), payload.sizeBytes(), payload.sha256());
    }

    public static DecodedImage inspectDataUrl(String dataUrl, AttachmentUsageType usageType) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "附件内容不能为空");
        }
        int comma = dataUrl.indexOf(',');
        if (!dataUrl.startsWith("data:") || comma < 0) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "附件必须是 data URL base64 格式");
        }
        String header = dataUrl.substring(5, comma).toLowerCase(Locale.ROOT);
        if (!header.endsWith(";base64")) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "附件必须使用 base64 编码");
        }
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(dataUrl.substring(comma + 1));
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "附件 base64 内容无效");
        }
        String declaredMime = header.substring(0, header.length() - ";base64".length());
        return new DecodedImage(bytes, ImagePayloadInspector.inspect(bytes, declaredMime, usageType));
    }

    public record ImagePayload(String mimeType, Long sizeBytes, String sha256) {}

    public record DecodedImage(byte[] bytes, ImagePayloadInspector.ImagePayload payload) {}
}
