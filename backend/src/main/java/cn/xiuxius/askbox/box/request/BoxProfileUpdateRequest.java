package cn.xiuxius.askbox.box.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class BoxProfileUpdateRequest {

    @NotBlank(message = "公开地址不能为空")
    @Pattern(regexp = "^[a-z0-9-]{1,50}$", message = "公开地址只能包含小写字母、数字和短横线")
    private String slug;

    @NotBlank(message = "箱子名称不能为空")
    @Size(max = 100, message = "箱子名称最多100个字符")
    private String displayName;

    @Size(max = 500, message = "简介最多500个字符")
    private String description;
}
