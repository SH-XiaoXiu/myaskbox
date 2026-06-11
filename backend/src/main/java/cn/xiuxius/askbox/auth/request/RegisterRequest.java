package cn.xiuxius.askbox.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 200, message = "邮箱最多200个字符")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    private String code;

    @Size(max = 100, message = "显示名最多100个字符")
    private String displayName;
}
