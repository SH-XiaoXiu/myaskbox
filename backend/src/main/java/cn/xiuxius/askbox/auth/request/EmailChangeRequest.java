package cn.xiuxius.askbox.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class EmailChangeRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 200, message = "邮箱不能超过200个字符")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
