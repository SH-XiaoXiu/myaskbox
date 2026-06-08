package cn.xiuxius.askbox.attachment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;

import lombok.Data;

@Data
public class AttachmentSaveRequest {

    @NotBlank(message = "附件名称不能为空")
    @Size(max = 100, message = "附件名称最多100个字符")
    private String name;

    @NotNull(message = "附件用途不能为空")
    private AttachmentUsageType usageType;

    @NotBlank(message = "附件内容不能为空")
    private String contentBase64;

    @Size(max = 50, message = "背景色最多50个字符")
    private String bg;

    private Integer sortOrder;
    private Boolean isActive;
}
