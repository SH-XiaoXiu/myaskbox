package cn.xiuxius.askbox.question.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ReplyTokenAnswerRequest {
    @NotBlank(message = "回答不能为空")
    @Size(max = 5000, message = "回答最多5000个字符")
    private String answer;
}
