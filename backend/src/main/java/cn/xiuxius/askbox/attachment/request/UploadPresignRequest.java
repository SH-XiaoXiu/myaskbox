package cn.xiuxius.askbox.attachment.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;

import lombok.Data;

@Data
public class UploadPresignRequest {

    @NotNull(message = "附件用途不能为空")
    private AttachmentUsageType usageType;

    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名最多255个字符")
    private String fileName;

    @NotBlank(message = "文件类型不能为空")
    @Size(max = 100, message = "文件类型最多100个字符")
    private String mimeType;

    @Min(value = 1, message = "文件大小必须大于0")
    private long sizeBytes;
}
