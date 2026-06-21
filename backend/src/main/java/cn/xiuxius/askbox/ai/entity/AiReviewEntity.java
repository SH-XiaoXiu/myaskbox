package cn.xiuxius.askbox.ai.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import cn.xiuxius.askbox.ai.enums.AiReviewStatus;
import cn.xiuxius.askbox.ai.enums.AiReviewTriggerType;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("ai_reviews")
@Data
@Accessors(chain = true)
public class AiReviewEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long questionId;
    private Long boxUserId;
    private Long answerId;
    private AiReviewStatus status;
    private String content;
    private String errorMessage;
    private String model;
    private String promptVersion;
    private AiReviewTriggerType triggerType;
    private Long triggeredBy;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    private OffsetDateTime completedAt;
}
