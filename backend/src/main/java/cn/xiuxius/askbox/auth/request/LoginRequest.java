package cn.xiuxius.askbox.auth.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class LoginRequest {
    @Email(message = "邮箱格式不正确")
    @Size(max = 200, message = "邮箱最多200个字符")
    private String email;

    @Email(message = "邮箱格式不正确")
    @Size(max = 200, message = "邮箱最多200个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @AssertTrue(message = "邮箱不能为空")
    public boolean isEmailProvided() {
        return hasText(email) || hasText(username);
    }

    public String getLoginEmail() {
        return hasText(email) ? email : username;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
