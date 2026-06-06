package cn.xiuxius.askbox.question.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import cn.xiuxius.askbox.question.enums.QuestionStatus;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("questions")
@Data
@Accessors(chain = true)
public class QuestionEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("box_user_id")
    private Long boxUserId;

    @TableField("avatar_id")
    private Long avatarId;

    private String question;
    private QuestionStatus status;
    private String ip;

    @TableField("user_agent")
    private String userAgent;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    public boolean isOwnedByBox(Long boxUserId) {
        return this.boxUserId != null && this.boxUserId.equals(boxUserId);
    }

    public boolean isPending() {
        return status == QuestionStatus.PENDING;
    }

    public boolean isDismissed() {
        return status == QuestionStatus.DISMISSED;
    }
}
