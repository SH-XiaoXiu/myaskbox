package cn.xiuxius.askbox.system.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class AdminUserUpdateRequest {

    @Size(max = 100, message = "显示名最多100个字符")
    private String displayName;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 200, message = "邮箱最多200个字符")
    private String email;
}
