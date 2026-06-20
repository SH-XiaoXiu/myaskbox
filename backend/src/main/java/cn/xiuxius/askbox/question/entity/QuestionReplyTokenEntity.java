package cn.xiuxius.askbox.question.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("question_reply_tokens")
@Data
@Accessors(chain = true)
public class QuestionReplyTokenEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("question_id")
    private Long questionId;

    @TableField("box_user_id")
    private Long boxUserId;

    @TableField("token_hash")
    private String tokenHash;

    @TableField("expires_at")
    private OffsetDateTime expiresAt;

    @TableField("used_at")
    private OffsetDateTime usedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
