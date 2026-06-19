package cn.xiuxius.askbox.auth.request;

import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(max = 100, message = "显示名称不能超过100个字符")
    private String displayName;

    private String avatarObjectKey;
}
