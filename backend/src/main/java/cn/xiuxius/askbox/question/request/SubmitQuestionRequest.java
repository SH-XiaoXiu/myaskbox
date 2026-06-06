package cn.xiuxius.askbox.question.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class SubmitQuestionRequest {
    @NotNull(message = "请选择一个头像")
    private Long avatarId;

    @NotBlank(message = "问题不能为空")
    @Size(min = 1, max = 350, message = "问题长度为1-350个字符")
    private String question;
}
