package cn.xiuxius.askbox.topic.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class TopicCreateRequest {
    @NotBlank(message = "话题标题不能为空")
    @Size(max = 24, message = "话题标题最多24个字符")
    private String title;

    @Size(max = 500, message = "话题简介最多500个字符")
    private String description;

    @NotNull(message = "到期时间不能为空")
    private OffsetDateTime expiresAt;
}
