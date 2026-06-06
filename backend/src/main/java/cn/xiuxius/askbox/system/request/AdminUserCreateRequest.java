package cn.xiuxius.askbox.system.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class AdminUserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名最多50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
    private String password;

    @Size(max = 100, message = "显示名最多100个字符")
    private String displayName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 200, message = "邮箱最多200个字符")
    private String email;
}
