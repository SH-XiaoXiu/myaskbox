package cn.xiuxius.askbox.answer.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("answers")
@Data
@Accessors(chain = true)
public class AnswerEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("question_id")
    private Long questionId;

    private String content;

    @TableField("answered_by")
    private Long answeredBy;

    private String ip;

    @TableField("user_agent")
    private String userAgent;

    private OffsetDateTime createdAt;
}
