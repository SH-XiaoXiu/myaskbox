package cn.xiuxius.askbox.avatar.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class AvatarSaveRequest {

    @NotBlank(message = "头像名称不能为空")
    @Size(max = 100, message = "头像名称最多100个字符")
    private String name;

    @NotBlank(message = "头像内容不能为空")
    private String iconBase64;

    @NotBlank(message = "背景色不能为空")
    @Size(max = 50, message = "背景色最多50个字符")
    private String bg;

    private Integer sortOrder;
}
