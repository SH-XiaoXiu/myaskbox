package cn.xiuxius.askbox.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度必须为 8-64 位")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
